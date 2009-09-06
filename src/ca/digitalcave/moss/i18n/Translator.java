/*
 * Created on May 8, 2006 by wyatt
 */
package ca.digitalcave.moss.i18n;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import ca.digitalcave.moss.common.ClassLoaderFunctions;


public class Translator {
	
	protected final Properties translations = new Properties();
	
	private final String translationSuffix;
	
	public Translator(String translationSuffix) {
		this.translationSuffix = translationSuffix;
	}

	/**
	 * We try to load the languages specified in the list, in order of index.
	 * This allows languages to be a subset of another; for instance, if the
	 * entire program is translated into English, but the other languages
	 * or localizations are incomplete, you can load English first, and then
	 * load the localizations to overwrite all translated items.
	 * 
	 * This gives us extreme flexibility and maintainability when 
	 * creating locales - to change a single term, you only have to 
	 * create a .lang file with that one term changed.
	 * 
	 * @param resourcePath The path to the languages, inside of the current .jar file.  Should be absolute (i.e., starting with a slash).
	 * @param languages A list of languages to load, in order of index.
	 * @param translationSuffix The suffix of the translation files, including the period if applicable.
	 */
	public void loadLanguages(String resourcePath, List<String> languages) {
		for (String language : languages) {
			String path = resourcePath + "/" + language + translationSuffix;
			Logger.getLogger(this.getClass().getName()).finest("Loading translation from " + path);
			try {
				translations.load(Translator.class.getResourceAsStream(path));
			}
			catch (RuntimeException re){
				Logger.getLogger(this.getClass().getName()).finest("Could not load " + language + ": " + re);
			}
			catch (IOException ioe){
				Logger.getLogger(this.getClass().getName()).finest("Could not load " + language + ": " + ioe);
			}

		}

		setLocale();
	}

	/**
	 * We try to load the languages specified in the list, in order of index.
	 * This allows languages to be a subset of another; for instance, if the
	 * entire program is translated into English, but the other languages
	 * or localizations are incomplete, you can load English first, and then
	 * load the localizations to overwrite all translated items.
	 * 
	 * This gives us extreme flexibility and maintainability when 
	 * creating locales - to change a single term, you only have to 
	 * create a .lang file with that one term changed.
	 * 
	 * @param jarFile The jar file to load the translations from.
	 * @param resourcePath The path to the languages, inside of the current .jar file.  Should be absolute (i.e., starting with a slash).
	 * @param languages A list of languages to load, in order of index.
	 * @param translationSuffix The suffix of the translation files, including the period if applicable.
	 */
	public void loadLanguages(File jarFile, String resourcePath, List<String> languages) {
		for (String language : languages) {
			String path = resourcePath + "/" + language + translationSuffix;
			Logger.getLogger(this.getClass().getName()).finest("Loading translation from " + path + " in " + jarFile.getName());
			try {
				InputStream is = ClassLoaderFunctions.getResourceAsStreamFromJar(jarFile, path);
				if (is != null)
					translations.load(is);
				else
					Logger.getLogger(this.getClass().getName()).finest("Failed to load translation " + path + " in " + jarFile.getName() + "; could not open stream.");
			}
			catch (IOException ioe){
				Logger.getLogger(this.getClass().getName()).info("Could not load " + language + " from " + jarFile.getName() + ":/" + path + ": " + ioe);
			}

		}

		setLocale();		
	}

	/**
	 * We try to load the languages specified in the list, in order of index.
	 * This allows languages to be a subset of another; for instance, if the
	 * entire program is translated into English, but the other languages
	 * or localizations are incomplete, you can load English first, and then
	 * load the localizations to overwrite all translated items.
	 * 
	 * This gives us extreme flexibility and maintainability when 
	 * creating locales - to change a single term, you only have to 
	 * create a .lang file with that one term changed.
	 * 
	 * @param languageDirectory The path to the languages, on the current file system.
	 * @param languages A list of languages to load, in order of index.
	 * @param translationSuffix The suffix of the translation files, including the period if applicable.
	 */
	public void loadLanguages(File languageDirectory, List<String> languages) {
		if (languageDirectory == null)
			return;

		if (!languageDirectory.exists())
			languageDirectory.mkdirs();

		
		if (!languageDirectory.isDirectory())
			languageDirectory = languageDirectory.getParentFile();

		//We need to do this again in case the getParent returned null. 
		if (languageDirectory == null)
			return;
		
		for (String language : languages) {
			File languageFile = new File(languageDirectory.getAbsolutePath() + File.separator + language + translationSuffix);
			try {
				translations.load(new BufferedInputStream(new FileInputStream(languageFile)));
			}
			catch (IOException ioe){
				Logger.getLogger(this.getClass().getName()).info("Could not load " + language + " from " + languageFile.getAbsolutePath() + ": " + ioe);
			}
		}

		setLocale();
	}
	
	/**
	 * Returns a list of languages to try to load, based on the given language.
	 * This allows us to load partial translations and differences (dialects).
	 * This list can be passed directly to the loadLanguages() method in Translator.
	 * @param translation The localized language
	 * @return
	 */
	public List<String> getLanguageList(String translation){
		List<String> languageList = new LinkedList<String>();
		
		//English
		languageList.add("English");
		
		//Base Language (e.g., Espanol)
		languageList.add(translation.replaceAll("_\\(.*\\)$", ""));

		//Localized Language (e.g., Espanol_(MX))
		languageList.add(translation);
		
		return languageList;
	}


	/**
	 * Sets the locale according to the translations loaded so far.
	 */
	private void setLocale(){
		String localeLanguage = this.get(Keys.LOCALE_LANGUAGE_CODE);
		String localeCountry = this.get(Keys.LOCALE_COUNTRY_CODE);
		String localeVariant = this.get(Keys.LOCALE_VARIANT_CODE);

		if (localeLanguage == null) localeLanguage = "";
		if (localeCountry == null) localeCountry = "";
		if (localeVariant == null) localeVariant = "";

		Locale.setDefault(new Locale(localeLanguage, localeCountry, localeVariant));
	}

	/**
	 * Returns the translation, based on the given string.
	 * @param key The key to translate
	 * @return The translation in currently loaded language
	 */
	public String get(String key){
		if (key == null){
			return key;
		}
		String ret = translations.getProperty(key);
		if (ret == null)
			return key;

		return ret;
	}

	/**
	 * Returns the translation, based on the given TranslateKey.
	 * @param key The key to translate
	 * @return The translation in currently loaded language
	 */
	public String get(Enum<?> key){
		String ret = translations.getProperty(key.toString());
		if (ret == null)
			return key.toString();
		return ret;
	}
	
	/**
	 * Returns a copy of the properties backing file.  Used by the Language Editor.
	 * @return
	 */
	Properties getTranslations(){
		return (Properties) translations.clone();
	}
	
	/**
	 * Writes a value for a key, overwriting the existing value if it already exists.
	 * Used for the Language Editor to save changes.
	 * @param key
	 * @param value
	 */
	void setTranslation(String key, String value){
		translations.setProperty(key, value);
	}
	
	/**
	 * Removes a value for the key, if it exists.  Used for the Language Editor.
	 * @param key
	 */
	void removeTranslation(String key){
		translations.remove(key);
	}
}
