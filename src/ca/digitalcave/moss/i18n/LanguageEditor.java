/*
 * Created on Aug 11, 2007 by wyatt
 */
package ca.digitalcave.moss.i18n;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.homeunix.thecave.moss.application.document.exception.DocumentSaveException;
import org.homeunix.thecave.moss.collections.CompositeList;
import org.homeunix.thecave.moss.collections.FilteredList;
import org.homeunix.thecave.moss.collections.ListSet;
import org.homeunix.thecave.moss.common.OperatingSystemUtil;
import org.homeunix.thecave.moss.swing.LookAndFeelUtil;
import org.homeunix.thecave.moss.swing.MossDocumentFrame;
import org.homeunix.thecave.moss.swing.MossHintTextArea;
import org.homeunix.thecave.moss.swing.MossMenu;
import org.homeunix.thecave.moss.swing.MossMenuBar;
import org.homeunix.thecave.moss.swing.MossMenuItem;
import org.homeunix.thecave.moss.swing.MossPanel;
import org.homeunix.thecave.moss.swing.MossSearchField;
import org.homeunix.thecave.moss.swing.MossSearchField.SearchTextChangedEvent;
import org.homeunix.thecave.moss.swing.model.BackedListModel;

/**
 * A fairly complete standalone translation editor.  This allows people to easily
 * translate terms in a program.  It is designed to work well with the Moss
 * Translator objects, but it should be fine anywhere that a programmer uses
 * enumerations (or a list of string constants, although that is much harder for
 * the programmer) for a list of keys to translate.
 * 
 * To run the language editor, use code similar to the following:
 * 
 * 
 * <code>
 * try {
 * 		LanguageEditor editor = new LanguageEditor(".lang");
 * 		editor.loadKeys((Enum[]) FirstEnum.values());
 * 		editor.loadKeys((Enum[]) SecondEnum.values());
 * 		...
 * 		editor.loadLanguages(File pathToLanguages, "English", "Espanol", ...);
 * 		...
 * 		editor.openWindow();
 * }
 * catch (WindowOpenException woe){}
 * 
 * You cannot load more keys or languages once you have opened the window (well, you 
 * probably could, but I don't know what would happen, so don't try it).
 * 
 * Once you start the language editor, you will see a list of keys along the left side.
 * Click on each one in turn, and enter the translation in the correct language's text
 * area.  It saves to memory automatically when you click on a new key.  Once you are 
 * complete, you can programmatically call the save() or save(File) method.  It is 
 * generally recommended to extend this class, and add a menu option or button for your
 * users to save their changes.
 * 
 * When you save, it will output a translation .properties file for each of the languages
 * you loaded initially (using the loadLanguages() methods).  This file will include the 
 * original file, along with all changes you made in this session.
 * 
 * This file (along with the rest of Moss) is written by Wyatt Olson, and released under
 * the GPL.  If you wish to include this software in a commercial project, please contact
 * me (wyatt.olson@gmail.com), and we can discuss the possibility of purchasing a different
 * license more suitable for your needs.
 * 
 * @author wyatt
 *
 */
public class LanguageEditor extends MossDocumentFrame {
	public static final long serialVersionUID = 0;

	private final Map<String, Translator> translators = new HashMap<String, Translator>();
	private final Map<String, TranslatorPanel> translatorPanels = new HashMap<String, TranslatorPanel>();
	private final Map<String, String> languageColor = new HashMap<String, String>();
	private final Set<String> languageSet = new HashSet<String>();
	private final String translationSuffix;
	private final String primaryLanguage;
	private final JRadioButton filterAll;
	private final ButtonGroup filterGroup = new ButtonGroup();

	private final JList keyList;
	private final MossSearchField search;
	
	private final Set<String> enumStrings = new HashSet<String>();	//Keys which are from enums.  Keys which are from translation, but not enum, are marked in red.
	private final Set<String> translationStrings = new HashSet<String>();
	
	private final ListSet<String> enumStringsList = new ListSet<String>(enumStrings);
	private final ListSet<String> translationStringsList = new ListSet<String>(translationStrings);
	
	private final CompositeList<String> backingList;
	private final FilteredKeyListModel filter;
	private final StringBackedListModel keyListModel;
	private static final LanguageEditorDocument document = new LanguageEditorDocument();
	
	private String selectedFilterLanguage;
	
	public LanguageEditor() {
		this(".lang", null);
	}
	
	public LanguageEditor(String translationSuffix) {
		this(translationSuffix, null);
	}

	/**
	 * Creates a new LanguageEditor pane, with the specified translation suffix, primary
	 * language, and embedded state.  
	 * @param translationSuffix The suffix to append to each language when reading / writing.
	 * @param primaryLanguage If set, we disable editing on languages other than this one
	 */
	public LanguageEditor(String translationSuffix, String primaryLanguage) {
		super(document, "LanguageEditor");
		LookAndFeelUtil.setLookAndFeel();

		this.translationSuffix = translationSuffix;
		this.primaryLanguage = primaryLanguage;
		
		if (OperatingSystemUtil.isMac())
			search = new MossSearchField();
		else
			search = new MossSearchField("<Search>");
		backingList = new CompositeList<String>(true, false);
		filter = new FilteredKeyListModel(backingList);
		keyListModel = new StringBackedListModel(filter);
		keyList = new JList(keyListModel);

		filterAll = new JRadioButton("All");
	}

	@Override
	public void init() {
		super.init();

		backingList.addList(enumStringsList);
		backingList.addList(translationStringsList);
		
		loadKeys((Enum[]) Keys.values());
		
		JScrollPane listScroller = new JScrollPane(keyList);

		filterAll.setSelected(true);
		filterAll.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (filterAll.isSelected()) {
					selectedFilterLanguage = null;
					filter.updateFilteredList();
					keyListModel.updateList();
				}
			}
		});

		filterGroup.add(filterAll);

		search.setPreferredSize(new Dimension(200, search.getPreferredSize().height));
		search.addSearchTextChangedEventListener(new MossSearchField.SearchTextChangedEventListener(){
			public void searchTextChangedEventOccurred(SearchTextChangedEvent evt) {
				filter.updateFilteredList();
				keyListModel.updateList();				
			}
		});
		
		JButton help = new JButton("Help");
		help.putClientProperty("Quaqua.Button.style", "help");
		
		help.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						LanguageEditor.this, 
						"Language Editor Help\n\n"
						+ "The list on the left shows the keys which need to\n"
						+ "be translated.  Keys in red are 'spurious translations';\n"
						+ "they exist in the loaded translation, but not in\n"
						+ "the list of keys.  (This is most likely a programmer\n"
						+ "error, but there are times when it is normal).\n\n"
						+ "The colored dots beside each key corresponds to the\n"
						+ "language colors.  If there is a dot in a given color\n"
						+ "beside a key, it means that key is not translated into\n"
						+ "that language.\n\n"
						+ "You can filter the list by searching, or by selecting\n"
						+ "a radio button associated with a language.  If you select\n"
						+ "that button, only keys which have not been translated into\n"
						+ "the given language will be shown.\n\n"
						+ "The Language Editor is part of the Moss software package\n"
						+ "by Wyatt Olson (http://moss.thecave.homeunix.org), originally\n"
						+ "written for Buddi.  Feel free to email wyatt.olson@gmail.com\n" 
						+ "if you are interested in using this library for your own software."
						, 
						"Help", 
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		

		JPanel filterPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		filterPanelRight.add(search);
		filterPanelRight.add(filterAll);
		
		JPanel filterPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
		filterPanelLeft.add(help);

		JPanel filterPanel = new JPanel(new BorderLayout());
		filterPanel.add(filterPanelRight, BorderLayout.EAST);
		filterPanel.add(filterPanelLeft, BorderLayout.WEST);
		
		JPanel translatorPanel = new JPanel();
		translatorPanel.setLayout(new GridLayout(0, 1));
		JScrollPane translatorScroller = new JScrollPane(translatorPanel);

		List<String> languages = new LinkedList<String>(this.languageSet);
		Collections.sort(languages);

		ColorChooser chooser = new ColorChooser();

		for (String language : languages) {
			languageColor.put(language, chooser.getNextColor());
			TranslatorPanel panel = new TranslatorPanel(language, (primaryLanguage == null ? true : primaryLanguage.equals(language)));
			translatorPanels.put(language, panel);
			translatorPanel.add(panel);
		}
		
		keyList.setCellRenderer(new DefaultListCellRenderer(){
			public static final long serialVersionUID = 0;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int row, boolean arg3, boolean arg4) {
				super.getListCellRendererComponent(list, value, row, arg3, arg4);

				String key = (String) value;
				String missingLanguages = "";
				String symbol = "\u25c9";
				for (String language : LanguageEditor.this.languageSet) {
					//If the translation is not set, we mark it as this color
					if (LanguageEditor.this.translators.get(language).get(key).equals(key))
						missingLanguages += "<font color='" + languageColor.get(language) + "'>" + symbol + "</font>";
					else
						missingLanguages += "<font color='white'>" + symbol + "</font>";
				}
				missingLanguages += "<font color='white'>" + symbol + "</font>";
				if (!enumStrings.contains(key))
					value = "<font color='red'>" + value + "</font>";
				this.setText("<html>" + missingLanguages + value + "</html>");

				return this;
			}
		});
		keyList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()){
					getDocument().setChanged();
					for (String string : LanguageEditor.this.languageSet) {
						translatorPanels.get(string).loadKey(keyList.getSelectedValue().toString());
					}
					updateContent();
				}
			}
		});
		
		backingList.updateList();
		
		this.setTitle("Language Editor");
		this.setLayout(new BorderLayout());
		this.add(filterPanel, BorderLayout.NORTH);
		this.add(listScroller, BorderLayout.WEST);
		this.add(translatorScroller, BorderLayout.CENTER);
		
		
		MossMenuBar menuBar = new MossMenuBar(this);
		MossMenu fileMenu = new MossMenu(this, "File");
		MossMenuItem save = new MossMenuItem(this, "Save Translation", KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		MossMenuItem close = new MossMenuItem(this, "Close Window", KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		save.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				try {
					LanguageEditor.this.save();
				}
				catch (DocumentSaveException dse){}
			}
		});
		close.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				LanguageEditor.this.closeWindow();
			}
		});
		
		fileMenu.add(save);
		fileMenu.add(close);
		menuBar.add(fileMenu);
		this.setJMenuBar(menuBar);	

	}

	@Override
	public void initPostPack() {
		super.initPostPack();
		keyList.requestFocusInWindow();
	}
	
	@Override
	public boolean canClose() {
		if (getDocument().isChanged()){
			int ret = JOptionPane.showConfirmDialog(
					this, 
					"There are unsaved changes.  Do you wish\nto save your changes before closing\nthe Language Editor window?", 
					"Unsaved Changes", 
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (ret == JOptionPane.YES_OPTION){
				try {
					save();
					return true;
				}
				catch (DocumentSaveException dse){
					return false;
				}
			}
			else if (ret == JOptionPane.NO_OPTION){
				return true;
			}
			else {
				return false;
			}
		}

		return true;
	}
	
	public void loadKeys(Enum<?>... keys){
		enumStrings.addAll(new EnumList(keys));
		
		enumStringsList.updateList();
	}
	
	public void loadKeys(String... keys){
		for (String string : keys) {
			enumStrings.add(string);
		}
		
		enumStringsList.updateList();
	}

	public void loadLanguages(String resourcePath, String... languages) {
		for (String language : languages) {
			this.languageSet.add(language);
			Translator t = translators.get(language);
			if (t == null){
				t = new Translator(translationSuffix);
				translators.put(language, t);
			}
			List<String> singleLanguageList = new LinkedList<String>();
			singleLanguageList.add(language);
			t.loadLanguages(resourcePath, singleLanguageList);
			
			for (Object string : t.getTranslations().keySet()) {
				translationStrings.add(string.toString());				
			}
			
			translationStringsList.updateList();
		}
	}

	public void loadLanguages(String resourcePath, File jarFile, String... languages) {
		for (String language : languages) {
			this.languageSet.add(language);
			Translator t = translators.get(language);
			if (t == null){
				t = new Translator(translationSuffix);
				translators.put(language, t);
			}
			List<String> singleLanguageList = new LinkedList<String>();
			singleLanguageList.add(language);
			t.loadLanguages(jarFile, resourcePath, singleLanguageList);
			
			for (Object string : t.getTranslations().keySet()) {
				translationStrings.add(string.toString());				
			}
			
			translationStringsList.updateList();
		}
	}

	public void loadLanguages(File languageDirectory, String... languages) {
		for (String language : languages) {
			this.languageSet.add(language);
			Translator t = translators.get(language);
			if (t == null){
				t = new Translator(translationSuffix);
				translators.put(language, t);
			}
			List<String> singleLanguageList = new LinkedList<String>();
			singleLanguageList.add(language);
			t.loadLanguages(languageDirectory, singleLanguageList);
			
			for (Object string : t.getTranslations().keySet()) {
				translationStrings.add(string.toString());				
			}
			
			translationStringsList.updateList();
		}
	}

//	/**
//	 * Saves the translations to the given directory.
//	 * @param f
//	 */
//	public void saveLanguages(File f){
//		//First we need to make sure changes are saved
//		for (String string : LanguageEditor.this.languageSet) {
//			translatorPanels.get(string).loadKey(keyList.getSelectedValue().toString());
//		}
//		
//		for (String language : languageSet) {
//			if (primaryLanguage == null || primaryLanguage.equals(language)){
//				try {
//					translators.get(language).getTranslations().store(new FileOutputStream(new File(f + File.separator + language + translationSuffix)), "Created by Wyatt Olson's Language Editor, included in Moss: http://moss.thecave.homeunix.org");
//				}
//				catch (FileNotFoundException fnfe){
//					Log.error(fnfe);
//				}
//				catch (IOException ioe){
//					Log.error(ioe);
//				}
//			}
//		}
//	}

	/**
	 * Save the translations.  The user will be prompted for a location.
	 */
//	public void saveLanguages(){
//		FileFilter filter = new FileFilter(){
//			@Override
//			public boolean accept(File f) {
//				if (f.isDirectory())
//					return true;
//				return false;
//			}
//
//			@Override
//			public String getDescription() {
//				return "Directories";
//			}
//		};
//
//		JFileChooser chooser = new JFileChooser();
//		chooser.setFileFilter(filter);
//		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//
//		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
//			saveLanguages(chooser.getSelectedFile());
//		}
//	}

	String getSelectedFilterLanguage(){
		return selectedFilterLanguage;
	}

	public void save() throws DocumentSaveException {
		FileFilter filter = new FileFilter(){
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				return false;
			}

			@Override
			public String getDescription() {
				return "Directories";
			}
		};

		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
			saveAs(chooser.getSelectedFile());
		}
	}

	/**
	 * Saves the translations to the given directory.
	 * @param file
	 */
	public void saveAs(File directory) throws DocumentSaveException {
		//First we need to make sure changes are saved
		for (String string : LanguageEditor.this.languageSet) {
			translatorPanels.get(string).loadKey(keyList.getSelectedValue().toString());
		}
		
		//If we have passed in a file, get the parent directory.
		if (!directory.isDirectory())
			directory = directory.getParentFile();

		//If the directory does not exist, attempt to create it
		if (!directory.exists())
			directory.mkdirs();
		
		//Save each translation to its own file.
		boolean success = true;
		for (String language : languageSet) {
			if (primaryLanguage == null || primaryLanguage.equals(language)){
				try {
					translators.get(language).getTranslations().store(
							new FileOutputStream(new File(
									directory 
									+ File.separator 
									+ language 
									+ translationSuffix)), 
					"Created by Wyatt Olson's Language Editor, included in Moss: http://moss.thecave.homeunix.org");
				}
				catch (IOException ioe){
					success = false;
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error saving languague: ", ioe);
					JOptionPane.showMessageDialog(this, 
							"Error saving language: " + ioe.getMessage() + "\n\nStack Trace:\n" + ioe, 
							"Error Saving Language", 
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		if (success)
			getDocument().resetChanged();
		
		updateContent();
	}

	private class TranslatorPanel extends MossPanel {
		public static final long serialVersionUID = 0;

		private final MossHintTextArea translation;
		private final String language;
		private final JRadioButton filterTranslated;
//		private final Translator translator;
//		private final Color color;
		private String currentlySelectedKey = null;

		public TranslatorPanel(String language, boolean enabled) {
			super(true);
			
//			this.translator = translators.get(language);
			this.language = language;
//			this.color = lang;

			filterTranslated = new JRadioButton("<html><font color='" + languageColor.get(language) + "'>" + getVerticalText(language) + "</font></html>");
			translation = new MossHintTextArea("", true);

			translation.setEditable(enabled);
			translation.setForeground(enabled ? Color.BLACK : Color.GRAY);
			
			open();
		}

		@Override
		public void init() {
			super.init();

			filterTranslated.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (filterTranslated.isSelected()) {
						selectedFilterLanguage = language;
						filter.updateFilteredList();
						keyListModel.updateList();
					}
				}
			});
			filterGroup.add(filterTranslated);

			JScrollPane scroller = new JScrollPane(translation);
			scroller.setPreferredSize(new Dimension(500, 150));

			this.setLayout(new BorderLayout());
			this.add(scroller, BorderLayout.CENTER);
			this.add(filterTranslated, BorderLayout.EAST);
		}

		public void loadKey(String key){
			//First we save the old value
			if (currentlySelectedKey != null){
				if (translation.getText().length() > 0)
					translators.get(language).setTranslation(currentlySelectedKey.toString(), translation.getText());
				else
					translators.get(language).removeTranslation(currentlySelectedKey.toString());
			}

			//Then we load the new one
			String value = translators.get(language).get(key);
			if (value.equals(key.toString()))
				translation.setText("");
			else
				translation.setText(value);

			currentlySelectedKey = key;
		}


		public String getValue(){
			return translation.getText();
		}

		private String getVerticalText(String text){
			String vertical = "";

			for (int i = 0; i < text.length(); i++){
				vertical += text.charAt(i) + "<br>";
			}

			return vertical;
		}
	}

	private class ColorChooser {
		private final String[] colors = {
				"#0000FF",
				"#00AA00",
				"#FF0000",
				"#AA00AA",
				"#AAAA00",
				"#00AAAA"
		};

		private int index = 0;

		public String getNextColor(){
			String ret = colors[index];
			index = (index + 1) % colors.length;

			return ret;
		}
	}

	private class FilteredKeyListModel extends FilteredList<String> {
		public final static long serialVersionUID = 0;

		public FilteredKeyListModel(List<String> filteredListSource) {
			super(filteredListSource);
		}

		@Override
		public boolean isIncluded(String object) {
			//If a language is selected, only return untranslated terms
			if (getSelectedFilterLanguage() != null){
				if (!translators.get(getSelectedFilterLanguage()).get(object).equals(object))
					return false;
			}
			//If no language is selected, return all.
			if (search.getText().length() > 0){
				for (String language : languageSet) {
					if (translators.get(language).get(object).toLowerCase().contains(search.getText().toLowerCase())
							|| object.toLowerCase().contains(search.getText().toLowerCase()))
						return true;
				}
				return false;
			}
			else
				return true;
		}
	}

	private class EnumList extends LinkedList<String> {
		public final static long serialVersionUID = 0;

		public EnumList(Enum<?>... enums) {
			addEnum(enums);
		}

		public void addEnum(Enum<?>... enums){
			for (Enum<?> e : enums) {
				this.add(e.toString());
			}
		}
	}

	private class StringBackedListModel extends BackedListModel<String> {
		public final static long serialVersionUID = 0;

		public StringBackedListModel(List<String> listModel) {
			super(listModel);
		}

		public void updateList(){
			fireContentsChanged(this, -1, -1);
		}
	}
}
