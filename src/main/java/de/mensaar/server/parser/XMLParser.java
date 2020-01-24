package de.mensaar.server.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.mensaar.data.Day;
import de.mensaar.data.Meal;
import de.mensaar.data.SpecialString;

public class XMLParser {

	private final static HashMap<String, String> abbreviations = new HashMap<String, String>();

	static {
		abbreviations.put("1", "mit Farbstoff");
		abbreviations.put("2", "mit Konservierungsstoffe");
		abbreviations.put("3", "mit Antioxidationsmittel");
		abbreviations.put("4", "mit Geschmacksverstärker");
		abbreviations.put("5", "geschwefelt");
		abbreviations.put("6", "geschwärzt");
		abbreviations.put("7", "gewachst");
		abbreviations.put("8", "mit Phosphat");
		abbreviations.put("9", "mit Süßungsmittel");
		abbreviations.put("10", "enthält eine Phenylalaninquelle");
		abbreviations.put("11", "mit Milcheiweiß");
		abbreviations.put("12", "mit Geflügelanteil");
		abbreviations.put("13", "mit Schweinefleisch");
		abbreviations.put("14", "mit Alkohol");
		abbreviations.put("15", "kann Spuren von Nüssen enthalten");
		abbreviations.put("16", "ohne Schweinefleisch");
		abbreviations.put("17", "mit Knoblauch");
		abbreviations.put("18", "Vegetarisch");
		abbreviations.put("19", "Biologisches Essen");
		abbreviations.put("20", "Backtriebmittel");
		abbreviations.put("21", "laktosefrei");
		abbreviations.put("22", "veganes Essen");
		abbreviations.put("23", "Fisch aus nachhaltigem Fang");

		abbreviations.put("Gl", "Glutenhaltiges Getreide");
		abbreviations.put("Kr", "Krebstiere, Krusten- und Schalentiere");
		abbreviations.put("Ei", "Ei");
		abbreviations.put("Fi", "Fisch");
		abbreviations.put("En", "Erdnüsse");
		abbreviations.put("So", "Soja");
		abbreviations.put("La", "Milch und Lactose");
		abbreviations.put("Nu", "Schalenfrüchte (Nüsse)");
		abbreviations.put("Sl", "Sellerie");
		abbreviations.put("Sf", "Senf");
		abbreviations.put("Se", "Sesamsamen");
		abbreviations.put("Sw", "Schwefeldioxid/Sulfite");
		abbreviations.put("Lu", "Lupinen");
		abbreviations.put("Wt", "Weichtiere");
	}

	public static void main(String argv[]) {

	}

	// public static Day[] parseFile(File fXmlFile) throws SAXException, IOException, ParserConfigurationException {
	public static Day[] parseFile(InputStream fXmlFile) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		// Document doc = dBuilder.parse(fXmlFile);
		Document doc = dBuilder.parse(fXmlFile);

		doc.getDocumentElement().normalize();

		NodeList tagList = doc.getElementsByTagName("tag");

		Day[] dayList = new Day[tagList.getLength()];

		for (int i = 0; i < tagList.getLength(); i++) {
			Node tagNode = tagList.item(i);

			if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
				Element tagElement = (Element) tagNode;

				Day day = parseDay(tagElement);
				dayList[i] = day;
			}
		}

		return dayList;
	}

	private static Day parseDay(Element tagElement) {
		String timestamp = tagElement.getAttribute("timestamp");

		NodeList itemList = tagElement.getElementsByTagName("item");

		Meal[] mealList = new Meal[itemList.getLength()];

		for (int i = 0; i < itemList.getLength(); i++) {
			Node itemNode = itemList.item(i);

			if (itemNode.getNodeType() == Node.ELEMENT_NODE) {

				Element itemElement = (Element) itemNode;

				Meal meal = parseMeal(itemElement);
				mealList[i] = meal;
			}
		}

		Day day = new Day();
		day.setTimestamp(timestamp);
		day.setMeals(mealList);

		return day;
	}
	
	private static String convertColorToRGB(String color) {
		String[] rgb = color.split(",");
		StringBuilder sb = new StringBuilder("#");
		
		for(String n : rgb) {
			int nInt = Integer.valueOf(n);
			String nStr = Integer.toHexString(nInt);
			if(nStr.length() < 2) {
				sb.append("0");
				sb.append(nStr);
			} else {
				sb.append(nStr);
			}			
		}
		
		return sb.toString();
	}

	private static Meal parseMeal(Element itemElement) {
		String category = stripString(itemElement.getElementsByTagName("category").item(0).getTextContent());
		String title = stripString(itemElement.getElementsByTagName("title").item(0).getTextContent());
		String description = stripString(itemElement.getElementsByTagName("description").item(0).getTextContent());
		String priceStudent = stripString(itemElement.getElementsByTagName("preis1").item(0).getTextContent());
		String priceWorker = stripString(itemElement.getElementsByTagName("preis2").item(0).getTextContent());
		String priceGuest = stripString(itemElement.getElementsByTagName("preis3").item(0).getTextContent());
		String color = stripString(itemElement.getElementsByTagName("color").item(0).getTextContent());
		
		color = convertColorToRGB(color);

		NodeList componentsList = itemElement.getElementsByTagName("component");
		NodeList beilagenList = itemElement.getElementsByTagName("beilage");
		NodeList kennzeichnungenList = itemElement.getElementsByTagName("kennzeichnung");

		SpecialString[] components = new SpecialString[componentsList.getLength()];

		for (int i = 0; i < componentsList.getLength(); i++) {
			Node componentNode = componentsList.item(i);

			if (componentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element componentElement = (Element) componentNode;

				components[i] = convertToSpecialString(stripString(componentElement.getTextContent()));
			}
		}

		String[] dishes = new String[beilagenList.getLength()];

		for (int i = 0; i < beilagenList.getLength(); i++) {
			Node beilagenNode = beilagenList.item(i);

			if (beilagenNode.getNodeType() == Node.ELEMENT_NODE) {
				Element beilagenElement = (Element) beilagenNode;

				dishes[i] = stripString(beilagenElement.getTextContent());
			}
		}

		String[] supplements = new String[kennzeichnungenList.getLength()];

		for (int i = 0; i < kennzeichnungenList.getLength(); i++) {
			Node kennzeichnungNode = kennzeichnungenList.item(i);

			if (kennzeichnungNode.getNodeType() == Node.ELEMENT_NODE) {
				Element kennzeichnungElement = (Element) kennzeichnungNode;

				supplements[i] = stripString(kennzeichnungElement.getTextContent());
			}
		}
		
		HashSet<String> allergens = new HashSet<String>();
		allergens.addAll(extractAllergens(title));
		allergens.addAll(extractAllergens(description));
		title = removeAllergens(title);
		description = removeAllergens(description);
		

		Meal meal = new Meal();
		meal.setCategory(category);
		meal.setTitle(title);
		meal.setDescription(description);
		meal.setPriceStudent(priceStudent);
		meal.setPriceWorker(priceWorker);
		meal.setPriceGuest(priceGuest);
		meal.setColor(color);

		meal.setComponents(components);
		meal.setDishes(dishes);
		meal.setSupplements(supplements);
		
		String[] allergensArr = new String[allergens.size()];
		allergens.toArray(allergensArr);
		meal.setAllergens(allergensArr);

		return meal;
	}
	
	private static String removeAllergens(String str) {
		if(str.isEmpty())
			return str;
		
		return str.replaceAll("\\(.*?\\)", "").replace(" ,", ",");
	}
	
	private static ArrayList<String> extractAllergens(String str) {
		if(str.isEmpty())
			return new ArrayList<String>();
		
		if(str.contains("(")) {
			boolean replace = false;
			char[] charArr = str.toCharArray();
			for (int i = 0; i < charArr.length; i++) {
				switch (charArr[i]) {
				case '(':
					replace = true;
					break;
				case ')':
					replace = false;
					break;
				case ',':
					if (replace)
						charArr[i] = ';';
					break;
				}
			}
			
			str = String.copyValueOf(charArr);
			
			String[] ingredients = str.split(",");
			ArrayList<String> allergens = new ArrayList<String>();
			
			for(int i = 0; i < ingredients.length; i++) {		
				if(!ingredients[i].contains("("))
					continue;
				
				int idxStart = ingredients[i].indexOf("(");
				int idxStop = ingredients[i].indexOf(")");
				String substring = ingredients[i].substring(idxStart, idxStop + 1);
				substring = substring.replace("(", "").replace(")", "");

				String[] split = substring.split(";");

				for (int j = 0; j < split.length; j++) {
					if(abbreviations.containsKey(split[j])) {
						split[j] = abbreviations.get(split[j]);
					}
					
					allergens.add(split[j]);
				}
			}
			
			return allergens;
		} 
			
		return new ArrayList<String>();
	}

	private static SpecialString convertToSpecialString(String str) {
		SpecialString ss = new SpecialString();

		if (str.contains("(")) {
			int idxStart = str.indexOf("(");
			int idxStop = str.indexOf(")");
			String value = str.substring(0, idxStart) + str.substring(idxStop + 1);
			String substring = str.substring(idxStart, idxStop + 1);
			substring = substring.replace("(", "").replace(")", "");

			String[] split = substring.split(",");

			for (int i = 0; i < split.length; i++) {
				split[i] = abbreviations.get(split[i]);
			}

			ss.setValue(value);
			ss.setAdditional(split);

		} else {
			ss.setValue(str);
		}
		
		return ss;
	}

	private static String stripString(String str) {
		return str.replace("\n", " ").replace("\r", " ").replace("\t", " ").replace("&amp;quot;", "\"").replace("&quot;", "\"").trim().replaceAll(" +", " ");
	}
}
