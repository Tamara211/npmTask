package task;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class FetchDependencies {

	static Map<Package, HashSet<Package>> savedPackagesMap = new HashMap<Package, HashSet<Package>>();

	public static void main(String[] args) throws Exception {

		// Insert package name and version to fetch dependencies
		printAllDependecies("array-first", "1.0.0");

	}

	// Main function which checks if this dependency is saved in the map and if so
	// returns its set of dependencies if not, calls a function to get the
	// dependencies
	public static void printAllDependecies(String packageName, String version) throws Exception {
		Package pack = new Package(packageName, version);
		if (!savedPackagesMap.containsKey(pack)) {
			savedPackagesMap.put(pack, getDependencies(pack));
		}
		printDependenciesMap();
	}

	// Calls the url in order to get the json which includes the dependencies
	public static JSONObject getJsonObjectFromURL(String packageName, String version) {
		String url = "https://registry.npmjs.org" + "/" + packageName + "/" + version;
		JSONObject json = new JSONObject();
		try {
			json = (JSONObject) new JSONTokener(IOUtils.toString(new URL(url))).nextValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}

	// Recursive function which looks for dependencies
	public static HashSet<Package> getDependencies(Package pack) throws Exception {
		if (savedPackagesMap.containsKey(pack)) {
			return savedPackagesMap.get(pack);
		}
		// Declare the result set and json objects
		HashSet<Package> result = new HashSet<Package>();
		JSONObject dependenciesJson = new JSONObject();
		JSONObject devDependenciesJson = new JSONObject();
		JSONObject combinedDependencies = new JSONObject();

		// Call the url and get the json
		JSONObject json = getJsonObjectFromURL(pack.getName(), pack.getVersion());
		if (json.length() != 0) {
			// Get the dependencies part from json
			if (json.has("dependencies")) {
				dependenciesJson = (JSONObject) json.get("dependencies");
			}
			if (json.has("devDependencies")) {
				devDependenciesJson = (JSONObject) json.get("devDependencies");
			}
			// combine both dependencies
			if (dependenciesJson.length() != 0 && devDependenciesJson.length() != 0) {
				combinedDependencies = mergeJSONObjects(dependenciesJson, devDependenciesJson);
			} else {
				combinedDependencies = (dependenciesJson.length() != 0) ? dependenciesJson : devDependenciesJson;
			}

			// Loop over every dependency
			Iterator<String> packagesIterator = combinedDependencies.keys();
			while (packagesIterator.hasNext()) {
				String key = packagesIterator.next();
				String version = (String) combinedDependencies.get(key);
				Character c = version.charAt(0);
				if (!Character.isDigit(c) && !Character.isLetter(c)) {
					version = version.substring(1);
				}
				Package newPackage = new Package(key, version);
				result.add(newPackage);
				if (savedPackagesMap.containsKey(newPackage)) {
					if (savedPackagesMap.get(key) != null) {
						result.addAll(savedPackagesMap.get(key));
					}
					return savedPackagesMap.get(key);
				} else {
					printAllDependecies(newPackage.getName(), newPackage.getVersion());
				}
			}
		}
		return result;
	}

	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
		JSONObject mergedJSON = new JSONObject();
		try {
			mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
			for (String crunchifyKey : JSONObject.getNames(json2)) {
				mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
			}

		} catch (JSONException e) {
			throw new RuntimeException("JSON Exception" + e);
		}
		return mergedJSON;
	}

	public static void printDependenciesMap() {
		for (Map.Entry<Package, HashSet<Package>> entry : savedPackagesMap.entrySet()) {
			System.out.println(entry.getKey().toString());
			if (entry.getValue() != null) {
				System.out.println("Dependencies:");
				for (Package p : entry.getValue()) {
					System.out.print(p.toString() + " --- ");
				}
			}
		}
	}
}
