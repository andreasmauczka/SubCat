package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.BkTree;
import at.ac.tuwien.inso.subcat.utility.commentparser.CommentNode;
import at.ac.tuwien.inso.subcat.utility.commentparser.ContentNodeVisitor;
import at.ac.tuwien.inso.subcat.utility.commentparser.Finder;
import at.ac.tuwien.inso.subcat.utility.commentparser.ParagraphNode;
import at.ac.tuwien.inso.subcat.utility.commentparser.Parser;
import at.ac.tuwien.inso.subcat.utility.commentparser.QuoteNode;
import at.ac.tuwien.inso.subcat.utility.phonetic.DirectHashFunc;
import at.ac.tuwien.inso.subcat.utility.phonetic.HashFunc;
import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;
import at.ac.tuwien.inso.subcat.utility.sentiment.SentimentAnalyser;
import at.ac.tuwien.inso.subcat.utility.sentiment.SentimentBlock;


public class CommentAnalyserTask extends PostProcessorTask {
	private final boolean matchWithMails = false;

	private SentimentAnalyser<Identity> analyser;
	private Parser<Identity> parser;
	private Finder<Identity> finder;
	private Model model;

	private HashFunc hashFunc = new DirectHashFunc ();
	private int nameWordDistance = 0;


	private class CommentAnalyser extends ContentNodeVisitor<Identity> {
		private LinkedList<CommentNode<Identity>> analysedComments;
		private List<Comment> comments;
		private PostProcessorException exceptionTunnel;
		private Identity commentIdentity;
		private Identity globalIdentity;
		private Project project;
		private HashMap<Integer, Identity> authorsById;
		private BkTree<Identity> authors;
		private Identity lastIdentity;

		private List<SentimentBlock<Identity>> sentimentBlocks;


		public Sentiment<Identity> analyse (CommentNode<Identity> commentNode, Comment comment, Project project, List<Comment> comments, LinkedList<CommentNode<Identity>> analysedComments, HashMap<Integer, Identity> authorsById, BkTree<Identity> authors) throws PostProcessorException {
			this.sentimentBlocks = new LinkedList<SentimentBlock<Identity>> ();
			this.commentIdentity = comment.getIdentity ();
			this.analysedComments = analysedComments;
			this.project = project;
			this.comments = comments;
			this.authors = authors;
			this.authorsById = authorsById;

			commentNode.accept (this);

			this.analysedComments = null;
			this.project = null;
			this.comments = null;
			this.globalIdentity = null;
			this.commentIdentity = null;
			this.authorsById = null;
			this.lastIdentity = null;
			
			if (exceptionTunnel != null) {
				PostProcessorException tmp = exceptionTunnel;
				exceptionTunnel = null;
				throw tmp;
			}

			Sentiment<Identity> sentiment = new Sentiment<Identity> (this.sentimentBlocks);
			this.sentimentBlocks = null;
			return sentiment;
		}

		@Override
		public void visitComment (CommentNode<Identity> comment) {
			if (comment.getPatchId () > 0) {
				try {
					globalIdentity = model.getIdentityForAttachmentIdentifier (
						project,
						new Integer (comment.getPatchId ()).toString ());
				} catch (SQLException e) {
					exceptionTunnel = new PostProcessorException (e);
					return ;
				}
			}

			if (authorsById.size () == 2) {
				for (Identity id : authorsById.values ()) {
					if (id.getId () != commentIdentity.getId ()) {
						globalIdentity = id;
						break;
					}
				}
			}

			comment.acceptChildren (this);
		}

		@Override
		public void visitQuote (QuoteNode<Identity> quote) {
			lastIdentity = null;
			
			if (globalIdentity != null) {
				quote.setData (globalIdentity);
				return ;
			}

			if (authorsById.size () <= 2) {
				return ;
			}
			
			int commentReference = quote.getCommentReference ();
			if (commentReference >= 0 && commentReference < comments.size ()) {
				lastIdentity = comments.get (commentReference).getIdentity ();
				quote.setData (lastIdentity);
				return ;
			}

			String[] quoteContent = quote.getContent ();
			if (quoteContent.length > 0) {
				CommentNode<Identity> reference = contains (analysedComments, quoteContent);
				if (reference != null) {
					lastIdentity = reference.getData ();
					quote.setData (lastIdentity);
					return ;
				}
			}
		}

		@Override
		public void visitParagraph (ParagraphNode<Identity> para) {
			if (globalIdentity != null) {
				para.setData (globalIdentity);
			} else if (authorsById.size () == 1) {
				// do nothing
			} else if (lastIdentity != null) {
				para.setData (lastIdentity);
			} else {
				String[] words = para.getContent ().split (" ");
				for (String word : words) {
					List<Identity> identities = authors.get (hashFunc.hash (word.toLowerCase ()), nameWordDistance);
					if (identities.size () == 1 && commentIdentity.getId () != identities.get (0).getId ()) {
						para.setData (identities.get (0));
						break;
					}
				}
			}

			if (para.getParagraphSeparatorSize () > 1) {
				lastIdentity = null;
			}


			SentimentBlock<Identity> sB = analyser.get (para.getOriginalContent ());
			sentimentBlocks.add (sB);
		}

		private CommentNode<Identity> contains (LinkedList<CommentNode<Identity>> comments, String[] strv) {
			if (strv.length == 0) {
				return null;
			}
			
			int maxIndex = 0;
			int maxLength = strv[0].length ();

			for (int i = 1; i < strv.length; i++) {
				if (strv[i].length () > maxLength) {
					maxLength = strv[i].length ();
					maxIndex = i;
				}
			}
			
			Iterator<CommentNode<Identity>> iter = comments.descendingIterator ();
			while (iter.hasNext ()) {
				CommentNode<Identity> comment = iter.next ();
				if (finder.contains (comment, strv[maxIndex])) {
					return comment;
				}
			}

			return null;
		}
	}

	public CommentAnalyserTask () {
		super (PostProcessorTask.BEGIN | PostProcessorTask.BUG | PostProcessorTask.END);
	}

	public void setDistance (int dist) {
		this.nameWordDistance = dist;
	}

	public void setHashFunc (HashFunc func) {
		this.hashFunc = func;
	}


	@Override
	public void begin (PostProcessor processor) throws PostProcessorException {
		this.analyser = new SentimentAnalyser<Identity> ();
		this.parser = new Parser<Identity> ();
		this.finder = new Finder<Identity> ();
		
		try {
			this.model = processor.getModelPool ().getModel ();
			this.model.removeSentiment (processor.getProject ());
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		}
	}

	@Override
	public void end (PostProcessor processor) throws PostProcessorException {
		if (model != null) {
			model.close ();
		}
	}

	@Override
	public void bug (PostProcessor processor, Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		LinkedList<CommentNode<Identity>> analysedComments = new LinkedList<CommentNode<Identity>> ();

		BkTree<Identity> authors = new BkTree<Identity> (new BkTree.LevensteinFunc ());
		HashMap<Integer, Identity> authorsById = new HashMap<Integer, Identity> ();
		
		for (Comment comment : comments) {
			Identity author = comment.getIdentity ();
			Set<String> nameFragments = author.getNameFragments (matchWithMails);

			for (String frag : nameFragments) {
				authors.add (hashFunc.hash (frag), author);
			}

			CommentNode<Identity> commentNode = parser.parse (comment.getContent ());
			commentNode.setData (comment.getIdentity ());

			authorsById.put (author.getId (), author);
			CommentAnalyser analyser = new CommentAnalyser ();
			Sentiment<Identity> sentiment = analyser.analyse (commentNode, comment, processor.getProject (), comments, analysedComments, authorsById, authors);

			try {
				model.begin ();
				model.addSentiment (comment, sentiment);
				model.commit ();
			} catch (SQLException e) {
				throw new PostProcessorException (e);
			}

			analysedComments.add (commentNode);
		}

	}


	@Override
	public String getName () {
		return "comment-analyser";
	}


	/*
	public static void main (String[] args) {
		try {
			List<Comment> comments = new LinkedList<Comment> ();

			Project project = new Project (null, new Date (), "http://bugzilla.gnome.org", "valadoc", null);
			User user = new User (null, project , "Florian Brosch");
			Identity identity = new Identity (null, "bug", "flo.brosch@gmail.com", "flo brosch", user);
			Component component = new Component (null, project, "test");
			Priority priority = new Priority (null, project, "super-important");
			Severity severity = new Severity (null, project, "also-super");
			Bug bug = new Bug (null, "123", identity, component , "Title", new Date (), priority, severity);
			comments.add (new Comment (1, bug, new Date (), identity, ""
					 + "aa bb cc dd ee ff gg hh ii jj\n"
					 + ""));

			comments.add (new Comment (1, bug, new Date (), identity, ""
					 + "> Servus,      hallo.\n"
					 + ">\n"
					 + "> aa bb cc dd ee ff gg hh ii jj\n"
					 + "\n"
					 + "So ist das.\n"));

			CommentAnalyserTask task = new CommentAnalyserTask ();
			task.begin (null);
			task.bug (null, null, null, comments);
		} catch (PostProcessorException e) {
			e.printStackTrace();
		}
	}
	*/
}
