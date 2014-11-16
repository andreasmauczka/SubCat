package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.inso.subcat.miner.Settings;
import at.ac.tuwien.inso.subcat.model.Bug;
import at.ac.tuwien.inso.subcat.model.BugHistory;
import at.ac.tuwien.inso.subcat.model.Category;
import at.ac.tuwien.inso.subcat.model.Comment;
import at.ac.tuwien.inso.subcat.model.Commit;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.utility.Lemmatizer;
import at.ac.tuwien.inso.subcat.utility.classifier.Class;
import at.ac.tuwien.inso.subcat.utility.classifier.Classifier;
import at.ac.tuwien.inso.subcat.utility.classifier.Dictionary;


public class ClassificationTask extends PostProcessorTask {
	private Lemmatizer lemmatiser;

	private HashMap<Dictionary, Map<Class, Category>> commitCategories;
	private HashMap<Dictionary, Map<Class, Category>> bugCategories;

	public ClassificationTask () {
		super (PostProcessorTask.COMMIT | PostProcessorTask.BUG);
	}

	public void commit (PostProcessor processor, Commit commit) throws PostProcessorException {
		try {
			Settings settings = processor.getSettings ();
			Model model = processor.getModelPool ().getModel ();

			if (commitCategories == null) {
				commitCategories = new HashMap<Dictionary, Map<Class, Category>> ();
				prepareDictionary (processor, model, commitCategories, settings.srcDictionaries);
			}

			for (Dictionary dict : settings.srcDictionaries) {
				List<String> _document = lemmatiser.lemmatize (commit.getTitle ());
				Classifier classifier = new Classifier (dict);
				Class cl = classifier.classify (_document);
				
				Category category = commitCategories.get (dict).get (cl);
				model.addCommitCategory (commit, category);
			}
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		}
	}

	public void bug (PostProcessor processor, Bug bug, List<BugHistory> history, List<Comment> comments) throws PostProcessorException {
		try {
			Settings settings = processor.getSettings ();
			Model model = processor.getModelPool ().getModel ();

			if (bugCategories == null) {
				bugCategories = new HashMap<Dictionary, Map<Class, Category>> ();
				prepareDictionary (processor, model, bugCategories, settings.bugDictionaries);
			}

			for (Dictionary dict : settings.bugDictionaries) {
				List<String> _document = lemmatiser.lemmatize (bug.getTitle ());
				if (comments.size () > 0) {
					_document.addAll (lemmatiser.lemmatize (comments.get (0).getContent ()));
				}
				Classifier classifier = new Classifier (dict);
				Class cl = classifier.classify (_document);

				Category category = bugCategories.get (dict).get (cl);
				model.addBugCategory (bug, category);
			}
		} catch (SQLException e) {
			throw new PostProcessorException (e);
		}
	}

	private void prepareDictionary (PostProcessor processor, Model model, HashMap<Dictionary, Map<Class, Category>> categories, List<Dictionary> dictionaries) throws SQLException {
		if (lemmatiser == null) {
			lemmatiser = new Lemmatizer ();
		}
		
		for (Dictionary dict : dictionaries) {
			Map<Class, Category> dictHash = new HashMap<Class, Category> ();
			categories.put (dict, dictHash);
				
			at.ac.tuwien.inso.subcat.model.Dictionary modelDict = model.addDictionary (dict.getTitle (), processor.getProject ());
			for (Class cl : dict.getAbsoluteClasses ()) {
				at.ac.tuwien.inso.subcat.model.Category modelCat = model.addCategory (cl.getName (), modelDict);
				dictHash.put (cl, modelCat);
			}

			for (Class cl : dict.getRelativeClasses ()) {
				at.ac.tuwien.inso.subcat.model.Category modelCat = model.addCategory (cl.getName (), modelDict);
				dictHash.put (cl, modelCat);
			}
		}
	}

	@Override
	public String getName () {
		return "classification";
	}
}
