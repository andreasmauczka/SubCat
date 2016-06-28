package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.FileChange;
import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.ManagedFile;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ObjectCallback;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.Tuple;
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
	private HashMap<ManagedFile, HashSet<Identity>> fileInteractions;
	private SentimentAnalyser<Identity> analyser;
	private Parser<Identity> parser;
	private Finder<Identity> finder;

	private int commitUpdateCnt = 0;
	private int commitCnt = 0;

	private static class Stats {
		public Comment comment;
		public Sentiment sentiment;
		public LinkedList<AuthorStats> stats;

		public Stats (Comment comment, Sentiment sentiment,
				LinkedList<AuthorStats> stats) {
			super ();
			this.comment = comment;
			this.sentiment = sentiment;
			this.stats = stats;
		}

	}

	private class AuthorStats {
		public Identity from;
		public Identity to;

		public int quotations;
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
		super (PostProcessorTask.BEGIN | PostProcessorTask.BUG | PostProcessorTask.COMMIT | PostProcessorTask.END);
	}

	@Override
	public void begin (PostProcessor processor) throws PostProcessorException {
		this.analyser = new SentimentAnalyser<Identity> ();
		this.parser = new Parser<Identity> ();
		this.finder = new Finder<Identity> ();
		
		Model model = null;
		try {
			Project project = processor.getProject ();
			model = processor.getModelPool ().getModel ();
			fileInteractions = new HashMap<ManagedFile, HashSet<Identity>> ();
			commitUpdateCnt = model.getCommitSentimentState (project);
			commitCnt = 0;

			
			model.cleanBugClosedStats (project);
			final Model fm = model;

			model.foreachBugClosedStats (project, new ObjectCallback<Tuple<Identity,Identity>> () {
				@Override
				public boolean processResult (Tuple<Identity, Identity> item) throws SQLException, Exception {
					fm.addSocialStats (item.fst, item.snd, 0, 0, 0, 0, 0, 0, 1);
					return true;
				}
			});
		} catch (Exception e) {
			throw new PostProcessorException (e);
		} finally {
			if (model != null) {
				model.close ();
			}
		}
	}

	@Override
	public void end (PostProcessor processor) throws PostProcessorException {
		fileInteractions (processor);
		
		// Cleanup:
		this.fileInteractions = null;
		this.analyser = null;
		this.finder = null;
		this.parser= null;
	}

	private void fileInteractions (PostProcessor processor) throws PostProcessorException {
		// File Interactions:
		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();

			model.begin ();
			model.cleanFileInteractionStats (processor.getProject ());
			for (HashSet<Identity> identities : fileInteractions.values ()) {
				for (Identity from: identities) {
					for (Identity to : identities) {
						if (from.equals (to) == false) {
							model.addSocialStats (from, to, 0, 0, 0, 1, 0, 0, 0);
						}
					}
				}
			}
			model.commit ();
		} catch (SQLException e) {
			if (model != null) {
				try {
					model.rollback ();
				} catch (SQLException e1) {
				}
			}
			throw new PostProcessorException (e);		
		} finally {		
			if (model != null) {
				model.close ();
			}
		}
	}
	
	@Override
	public void bug (PostProcessor processor, Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		LinkedList<CommentNode<Identity>> analysedComments = new LinkedList<CommentNode<Identity>> ();
		HashMap<Integer, Identity> authorsById = new HashMap<Integer, Identity> ();
		HashSet<Identity> authorsNew = new HashSet<Identity> ();

		Model model = null;
		try {
			model = processor.getModelPool ().getModel ();
			int commentUpdateCnt = model.getBugSentimentState (bug);
			int commentCnt = 0;
			// Analyse all comments first to avoid long transactions:
			LinkedList<Stats> bugStats = new LinkedList<Stats> ();
			for (Comment comment : comments) {
				Identity author = comment.getIdentity ();
				authorsById.put (author.getId (), author);
				commentCnt++;

				if (commentCnt > commentUpdateCnt) {
					authorsNew.add (author);

					CommentNode<Identity> commentNode = parser.parse (comment.getContent ());
					commentNode.setData (comment.getIdentity ());
			
					CommentAnalyser analyser = new CommentAnalyser ();
					LinkedList<AuthorStats> stats = new LinkedList<AuthorStats> ();
					Sentiment sentiment = analyser.analyse (commentNode, comment, processor.getProject (), comments, analysedComments, authorsById, stats, model);
					analysedComments.add (commentNode);

					bugStats.add (new Stats (comment, sentiment, stats));
				}
			}

			HashSet<Identity> authorsOld = new HashSet<Identity> (authorsById.values ());
			authorsOld.removeAll (authorsNew);


			try {
				model.begin ();
				for (Stats bStats : bugStats) {
					model.addSentiment (bStats.sentiment);
					model.addBugCommentSentiment (bStats.comment, bStats.sentiment);
					for (AuthorStats stat : bStats.stats) {
						model.addSocialStats (stat.from, stat.to, stat.quotations, stat.patchesReviewed, 0, 0, 0, 0, 0);
					}
				}

				// Update old:
				for (Identity from : authorsOld) {
					for (Identity to : authorsNew) {
						model.addSocialStats (from, to, 0, 0, 1, 0, 0, 0, 0);
					}
				}

				// Add new:
				for (Identity from : authorsNew) {
					for (Identity to : authorsById.values ()) {
						if (from.equals (to) == false) {
							model.addSocialStats (from, to, 0, 0, 1, 0, 0, 0, 0);
						}
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
			if (model != null) {
				model.close ();
			}
		}
	}

	@Override
	public void commit (PostProcessor processor, Commit commit, List<FileChange> changes) throws PostProcessorException {
		commitCnt++;

		for (FileChange change : changes) {
			HashSet<Identity> modifications = fileInteractions.get (change.getFile ());
			if (modifications == null) {
				modifications = new HashSet<Identity> ();
				fileInteractions.put (change.getFile (), modifications);
			}
			
			modifications.add (commit.getAuthor ());
		}

		if (commitCnt > commitUpdateCnt) {
			String[] paras = commit.getTitle ().split ("\n[ \t]*\n");
			LinkedList<SentimentBlock> blocks = new LinkedList<SentimentBlock> (); 
			for (String para : paras) {
				SentimentBlock block = analyser.get (para.replace ('\n', ' '));
				blocks.add (block);
			}
	
			Model model = null;
			try {
				Sentiment sentiment = new Sentiment (blocks);
				model = processor.getModelPool ().getModel ();
				model.begin ();
				model.addSentiment (sentiment);
				model.addCommitSentiment (commit, sentiment);

				Identity from = commit.getCommitter ();
				Identity to = commit.getAuthor ();
				if (from.equals (to) == false) {
					model.addSocialStats (from, to, 0, 0, 0, 0, 0, 1, 0);
				}
				model.commit ();
			} catch (SQLException e) {
				if (model != null) {
					try {
						model.rollback ();
					} catch (SQLException e1) {
					}
				}
				throw new PostProcessorException (e);
			} finally {
				if (model != null) {
					model.close ();
				}
			}
			
		}
	}

	@Override
	public String getName () {
		return "comment-analyser";
	}
}
