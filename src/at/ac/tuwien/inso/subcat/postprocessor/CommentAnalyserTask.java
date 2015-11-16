package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.commentparser.CommentNode;
import at.ac.tuwien.inso.subcat.utility.commentparser.ContentNodeVisitor;
import at.ac.tuwien.inso.subcat.utility.commentparser.Finder;
import at.ac.tuwien.inso.subcat.utility.commentparser.ParagraphNode;
import at.ac.tuwien.inso.subcat.utility.commentparser.Parser;
import at.ac.tuwien.inso.subcat.utility.commentparser.QuoteNode;
import at.ac.tuwien.inso.subcat.utility.sentiment.Sentiment;
import at.ac.tuwien.inso.subcat.utility.sentiment.SentimentAnalyser;
import at.ac.tuwien.inso.subcat.utility.sentiment.SentimentBlock;


public class CommentAnalyserTask extends PostProcessorTask {

	private SentimentAnalyser<Identity> analyser;
	private Parser<Identity> parser;
	private Finder<Identity> finder;

	private int commentUpdateCnt = 0;
	private int commentCnt = 0;

	private class AuthorStats {
		private Identity from;
		private Identity to;

		private int quotations;
		public int patchesReviewed;
	}

	private class CommentAnalyser extends ContentNodeVisitor<Identity> {
		private LinkedList<CommentNode<Identity>> analysedComments;
		private List<Comment> comments;
		private PostProcessorException exceptionTunnel;
		private Identity commentIdentity;
		private HashMap<Integer, Identity> authorsById;
		private LinkedList<AuthorStats> stats;
		private Project project;
		private Model model;

		private List<SentimentBlock> sentimentBlocks;
		
		public Sentiment analyse (CommentNode<Identity> commentNode, Comment comment, Project project, List<Comment> comments, LinkedList<CommentNode<Identity>> analysedComments, HashMap<Integer, Identity> authorsById, LinkedList<AuthorStats> stats, Model model) throws PostProcessorException {
			this.sentimentBlocks = new LinkedList<SentimentBlock> ();
			this.commentIdentity = comment.getIdentity ();
			this.analysedComments = analysedComments;
			this.comments = comments;
			this.authorsById = authorsById;
			this.stats = stats;
			this.project = project;

			this.model = model;
			commentNode.accept (this);
			this.model = null;

			this.analysedComments = null;
			this.comments = null;
			this.commentIdentity = null;
			this.authorsById = null;
			this.stats = null;
			this.project = null;
	
			if (exceptionTunnel != null) {
				PostProcessorException tmp = exceptionTunnel;
				exceptionTunnel = null;
				throw tmp;
			}

			Sentiment sentiment = new Sentiment (this.sentimentBlocks);
			this.sentimentBlocks = null;
			return sentiment;
		}

		@Override
		public void visitComment (CommentNode<Identity> comment) {
			if (comment.getPatchId () > 0) {
				try {
					AuthorStats stats = new AuthorStats ();
					stats.from = commentIdentity;
					stats.to = model.getIdentityForAttachmentIdentifier (project, new Integer (comment.getPatchId ()).toString ());
					stats.patchesReviewed = 1;
					this.stats.add (stats);
				} catch (SQLException e) {
					exceptionTunnel = new PostProcessorException (e);
					return ;
				}
			}

			comment.acceptChildren (this);
		}

		@Override
		public void visitQuote (QuoteNode<Identity> quote) {
			if (exceptionTunnel != null) {
				return ;
			}

			int commentReference = quote.getCommentReference ();
			if (commentReference >= 0 && commentReference < comments.size ()) {
				AuthorStats stats = new AuthorStats ();
				stats.from = commentIdentity;
				stats.to = comments.get (commentReference).getIdentity ();
				stats.quotations = 1;
				this.stats.add (stats);
				return ;
			}

			if (authorsById.size () <= 2) {
				for (Identity identity : authorsById.values ()) {
					if (identity.getId ().equals (commentIdentity.getId ()) == false) {
						AuthorStats stats = new AuthorStats ();
						stats.from = commentIdentity;
						stats.to = identity;
						stats.quotations = 1;
						this.stats.add (stats);
						break;
					}
				}
				return ;
			}

			String[] quoteContent = quote.getContent ();
			if (quoteContent.length > 0) {
				CommentNode<Identity> reference = contains (analysedComments, quoteContent);
				if (reference != null) {
					AuthorStats stats = new AuthorStats ();
					stats.from = commentIdentity;
					stats.to = reference.getData ();
					stats.quotations = 1;
					this.stats.add (stats);
					return ;
				}
			}
		}

		@Override
		public void visitParagraph (ParagraphNode<Identity> para) {
			if (exceptionTunnel != null) {
				return ;
			}

			SentimentBlock sB = analyser.get (para.getOriginalContent ());
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

	@Override
	public void begin (PostProcessor processor) throws PostProcessorException {
		this.analyser = new SentimentAnalyser<Identity> ();
		this.parser = new Parser<Identity> ();
		this.finder = new Finder<Identity> ();
		
		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();
			commentUpdateCnt = model.getSentimentState (processor.getProject ());
			commentCnt = 0;
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		} finally {
			if (model != null) {
				model.close ();
			}
		}
	}

	@Override
	public void end (PostProcessor processor) throws PostProcessorException {
		this.analyser = null;
		this.finder = null;
		this.parser= null;
	}

	@Override
	public void bug (PostProcessor processor, Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		LinkedList<CommentNode<Identity>> analysedComments = new LinkedList<CommentNode<Identity>> ();
		HashMap<Integer, Identity> authorsById = new HashMap<Integer, Identity> ();

		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();

			// Analyse all comments first to avoid long transactions:
			LinkedList<Stats> bugStats = new LinkedList<Stats> ();
			for (Comment comment : comments) {
				if (commentCnt > commentUpdateCnt) {
					Identity author = comment.getIdentity ();
				
					CommentNode<Identity> commentNode = parser.parse (comment.getContent ());
					commentNode.setData (comment.getIdentity ());
			
					authorsById.put (author.getId (), author);
					CommentAnalyser analyser = new CommentAnalyser ();
					LinkedList<AuthorStats> stats = new LinkedList<AuthorStats> ();
					Sentiment sentiment = analyser.analyse (commentNode, comment, processor.getProject (), comments, analysedComments, authorsById, stats, model);		
					analysedComments.add (commentNode);

					bugStats.add (new Stats (comment, sentiment, stats));
				}
				commentCnt++;
			}
	
			try {
				model.begin ();
	
				for (Stats bStats : bugStats) {
					model.addSentiment (bStats.sentiment);
					model.addBugCommentSentiment (bStats.comment, bStats.sentiment);
					for (AuthorStats stat : bStats.stats) {
						model.addSocialStats (stat.from, stat.to, stat.quotations, stat.patchesReviewed);
					}
				}
				
				model.commit ();
			} catch (SQLException e) {
				try {
					model.rollback ();
				} catch (SQLException e1) {
				}
				throw e;
			}
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		} finally {
			model.close ();
		}
	}

	private static class Stats {
		public Comment comment;
		public Sentiment sentiment;
		public LinkedList<AuthorStats> stats;

		public Stats(Comment comment, Sentiment sentiment,
				LinkedList<AuthorStats> stats) {
			super ();
			this.comment = comment;
			this.sentiment = sentiment;
			this.stats = stats;
		}

	}

	@Override
	public String getName () {
		return "comment-analyser";
	}
}
