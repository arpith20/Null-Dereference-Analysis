package PAVpointerAnalysisPackage;

/**
 * @authors Sridhar Gopinath	-	g.sridhar53@gmail.com
 * @authors Arpith K			-	arpith@live.com		
 *
 * Null Pointer Dereference analysis,
 * Program Analysis and Verification Course, Fall - 2015,
 * Computer Science and Automation (CSA),
 * Indian Institute of Science (IISc),
 * Bangalore
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Data {

	private HashMap<String, HashMap<Integer, Boolean>> columnMarkings = new HashMap<String, HashMap<Integer, Boolean>>();
	private HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>> pointsToMap = new HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>>();

	// pp1 contains pp2, ie, pp2 is a subset of pp1
	public Boolean contains(String pp1, String pp2) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pointsToMap.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pointsToMap.get(pp2);
		if (al_col1 == null && al_col2 == null)
			return true;
		if (al_col1 == null || al_col2 == null)
			return false;

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : al_col2.entrySet()) {
			Integer col = entry.getKey();
			if (al_col1.containsKey(col)) {
				if (!contains(pp1, col, pp2, col))
					return false;
			} else
				return false;
		}
		return true;

	}

	// tested
	// returns true if pp1 dominates pp2, ie., all entries of pp2 is in pp1
	public Boolean contains(String pp1, Integer col1, String pp2, Integer col2) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pointsToMap.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pointsToMap.get(pp2);
		if (al_col1 == null && al_col2 == null)
			return true;
		if (al_col1 == null || al_col2 == null)
			return false;

		HashMap<String, ArrayList<String>> var_hash1 = al_col1.get(col1);
		HashMap<String, ArrayList<String>> var_hash2 = al_col2.get(col2);
		for (Map.Entry<String, ArrayList<String>> innerentry : var_hash2.entrySet()) {
			String var = innerentry.getKey();
			if (var_hash1.containsKey(var)) {
				ArrayList<String> al_pt1 = var_hash1.get(var);
				ArrayList<String> al_pt2 = innerentry.getValue();
				if (!al_pt1.containsAll(al_pt2)) {
					return false;
				}
			} else {
				return false;
			}

		}
		return true;

	}

	// Function to add a mapping from VARIABLE to POINTSTO
	// Removes BOT if already present
	// STRONG UPDATE
	// Returns TRUE if anything is updated in the map. Else, FALSE
	public Boolean add(String pPoint, Integer col, String var, String pointsTo) {
		verifyPPAndCol(pPoint, col, "add");

		HashMap<String, ArrayList<String>> var_hash = pointsToMap.get(pPoint).get(col);

		remove(pPoint, col, "bot");

		// Check if mapping is being added for the first time
		ArrayList<String> al_pointsto = var_hash.get(var);
		if (al_pointsto == null) {
			al_pointsto = new ArrayList<String>();
			al_pointsto.add(pointsTo);

			var_hash.put(var, al_pointsto);
			return true;
		} else {
			if (!al_pointsto.contains(pointsTo)) {
				al_pointsto.add(pointsTo);
				return true;
			} else
				return false;
		}
	}

	public void copyEntireMap(String pPoint, Integer col, HashMap<String, ArrayList<String>> map) {
		verifyPPAndCol(pPoint, col, "copyEntireMap");

		HashMap<String, ArrayList<String>> originalMap = pointsToMap.get(pPoint).get(col);
		if (originalMap == null) {
			// No Mapping exists. Create mapping for the first time
			originalMap = new HashMap<String, ArrayList<String>>(map);
		} else {
			// Clear the old HashMap and copy the new one
			originalMap.clear();
			originalMap.putAll(map);
		}
		return;
	}

	public void display() {
		String s_pointsto = "";
		if (pointsToMap == null)
			throw new NullPointerException("How is this even possible?");
		for (Map.Entry<String, HashMap<Integer, HashMap<String, ArrayList<String>>>> entry : pointsToMap.entrySet()) {
			String programPoint = entry.getKey();
			System.out.println("\n" + programPoint + ":  ");
			HashMap<Integer, HashMap<String, ArrayList<String>>> al_cols = entry.getValue();
			if (al_cols == null)
				continue;
			for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> middleentry : al_cols.entrySet()) {
				HashMap<String, ArrayList<String>> var_hash = middleentry.getValue();
				System.out.println(" " + middleentry.getKey() + ":  ");
				if (var_hash == null)
					continue;
				if (var_hash.containsKey("bot")) {
					System.out.println("bot");
					continue;
				}
				if (var_hash.containsKey("") && var_hash.size() == 1) {
					System.out.println("{}");
					continue;
				}
				for (Map.Entry<String, ArrayList<String>> innerentry : var_hash.entrySet()) {
					String variable = innerentry.getKey();
					if (variable.equals(""))
						continue;
					s_pointsto = variable;
					ArrayList<String> pointsto = innerentry.getValue();
					if (pointsto == null)
						continue;
					s_pointsto += " -> {";
					for (String s : pointsto) {
						s_pointsto += s + ", ";
					}
					s_pointsto = s_pointsto.substring(0, s_pointsto.length() - 2);
					s_pointsto += "}";
					System.out.println(s_pointsto);
				}
			}
		}

	}

	// This will mark the column COL under the program point PP
	public void mark(String pPoint, Integer col) {
		HashMap<Integer, Boolean> h = columnMarkings.get(pPoint);
		if (h == null) {
			throw new NullPointerException(
					"mark: " + pPoint + " " + col + " Program Point doesn't exist while trying to mark");
		}
		h.put(col, true);
	}

	// This will un-mark the column COL under the program point PP
	// CHECKED
	public void unmark(String pPoint, Integer col) {

		HashMap<Integer, Boolean> markings = columnMarkings.get(pPoint);
		if (markings == null)
			throw new NullPointerException("unmark: " + pPoint + " " + col
					+ " Program Point doesn't exist in columnMarkings while trying to unmark");

		if (markings.containsKey(col) == false)
			throw new NullPointerException(
					"unmark: " + pPoint + " " + col + " Column doesn't exist in columnMarkings while trying to unmark");

		markings.put(col, false);

		return;
	}

	// This will return TRUE if the column COL is maked under the program point
	// PP
	// With an assumption that initially everything is marked
	public Boolean marked(String pp, Integer col) {
		HashMap<Integer, Boolean> h = columnMarkings.get(pp);
		if (h == null) {
			throw new NullPointerException(
					"marked: " + pp + " " + col + " Program Point doesn't exist while trying check if marked");
		}
		return h.get(col);
	}

	// Return TRUE only if all the columns under a program point PP is unmarked
	//
	public Boolean checkAllColumnsUnmarked(String pPoint) {

		// Check if the program point exists
		HashMap<Integer, Boolean> markings = columnMarkings.get(pPoint);
		if (markings == null)
			throw new NullPointerException("checkAllColumnsUnmarked: " + pPoint + " Program Point doesn't exist");

		// Return FALSE even if one of the columns is marked
		for (Map.Entry<Integer, Boolean> entry : markings.entrySet()) {
			Boolean mark = entry.getValue();
			if (mark == true)
				return false;
		}
		return true;
	}

	// This will propagate the the hashMap MAP to the column COL at the program
	// point PPOINT
	// If MAP is BOT: If no mappings are there at PPOINT under COL, then set it
	// to BOT
	// Else, DO NOT change the mappings already present
	public Boolean propagate(String pPoint, Integer col, HashMap<String, ArrayList<String>> map) {
		verifyPPAndCol(pPoint, col, "propagate");

		// if (pPoint.equals("phiTest.11.16")) {
		// if (col == 0) {
		// System.out.println("Map at 11.16 is \n");
		//
		// System.out.println(map);
		// System.out.println("PP is\n");
		// displayProgramPointUnderCol(pPoint, col);
		// }
		// }

		Boolean flag = false;
		if (map.containsKey("bot")) {

			// Check if the COL under PPOINT is NULL
			if (isNullMap(pPoint, col) || pointsToMap.get(pPoint).get(col).size() == 0) {
				setToBOT(pPoint, col);
				mark(pPoint, col);
				return true;
			} else if (!isBOT(pPoint, col))
				return false;
		}

		// Remove BOT entry from the next program point
		// Also remove "" mapping if it is there
		HashMap<String, ArrayList<String>> oldMap = retrieve(pPoint, col);
		if (oldMap.containsKey("bot"))
			remove(pPoint, col, "bot");

		for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			String var = entry.getKey();
			ArrayList<String> pointsto = entry.getValue();

			for (String pt : pointsto) {
				if (pt.equals(""))
					continue;
				if (add(pPoint, col, var, pt)) {
					mark(pPoint, col);
					flag = true;
				}
			}
		}

		// if (pPoint.equals("phiTest.16.17")) {
		// if (col == 0) {
		//// System.out.println("Map at 16.17 is \n");
		////
		//// System.out.println(map);
		//// System.out.println("\n");
		// displayProgramPointUnderCol(pPoint, col);
		// }
		// }
		return flag;
	}

	public void remove(String pp1, Integer col, String var) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pointsToMap.get(pp1);
		if (al_col1 != null) {
			HashMap<String, ArrayList<String>> h = al_col1.get(col);
			if (h != null) {
				if (h.containsKey(var))
					h.remove(var);
			}
		}
	}

	// Retrieves the pointsTo set of the variable VAR under the column COL at
	// the
	// program point PPOINT
	// If the pointsTo set is empty, return NULL
	// Else, return a COPY of the ArrayList
	public ArrayList<String> retrieve(String pPoint, Integer col, String var) {
		verifyPPAndCol(pPoint, col, "retrieve ArrayList");

		if (pointsToMap.get(pPoint).get(col) == null)
			System.out.println("HashMap is null");
		// Check if the pointsTo set of VAR is empty
		ArrayList<String> map = pointsToMap.get(pPoint).get(col).get(var);
		if (map == null)
			return null;
		else
			return new ArrayList<String>(pointsToMap.get(pPoint).get(col).get(var));
	}

	// Retrieves the mappings of all the variables under the column COL at the
	// program point PPOINT
	// This is a COPY of the HashMap
	// CHECKED
	public HashMap<String, ArrayList<String>> retrieve(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "retrieve HashMap");

		return new HashMap<String, ArrayList<String>>(pointsToMap.get(pPoint).get(col));
	}

	// Function to get the column markings HashMap at a particular program point
	// PPOINT
	// CHECKED
	public HashMap<Integer, Boolean> getColumnMarkings(String pPoint) {

		HashMap<Integer, Boolean> markings = columnMarkings.get(pPoint);
		if (markings == null)
			throw new NullPointerException(
					"getColumnMarkings: " + pPoint + " program point not present in columnMarkings");

		return markings;
	}

	// Function to add a column to the program point PPOINT
	// CHECKED
	public void openColumn(String pPoint, Integer col) {

		// Check if the program point is present
		HashMap<Integer, HashMap<String, ArrayList<String>>> colMap = pointsToMap.get(pPoint);
		if (colMap != null) {

			// Add the new column
			HashMap<String, ArrayList<String>> varMap = new HashMap<String, ArrayList<String>>();
			colMap.put(col, varMap);
		} else
			throw new NullPointerException(
					"openColumn: " + pPoint + " " + col + " programPoint not present in pointsToMap");

		// Open a column in the MARKED data structure as well and MARK it
		HashMap<Integer, Boolean> markings = columnMarkings.get(pPoint);
		if (markings == null)
			throw new NullPointerException("openColun: " + pPoint + " " + col
					+ " program point not present in columnMarkings when adding a column to it");
		else
			markings.put(col, true);

		return;
	}

	/**
	 * Function to add a new program point to DATA // Note: This will NOT create
	 * any columns at the program points // Columns will be added to the program
	 * points once addColumn is called // CHECKED
	 */
	public void addProgramPoint(String pPoint) {

		// Check if the program point is already present
		HashMap<Integer, HashMap<String, ArrayList<String>>> colMap = pointsToMap.get(pPoint);
		if (colMap == null) {
			colMap = new HashMap<Integer, HashMap<String, ArrayList<String>>>();

			// Add the program point to the MAIN HashMap
			pointsToMap.put(pPoint, colMap);

			// Create and add a HashMap for this program point in MARKED as well
			HashMap<Integer, Boolean> markings = new HashMap<Integer, Boolean>();
			columnMarkings.put(pPoint, markings);
		} else
			throw new NullPointerException(
					"addProgramPoint: " + pPoint + "programPoint already present in pointsToMap");

		return;
	}

	// Method to make a column at a particular program point to be mapped to BOT
	public void setToBOT(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "setToBot");

		HashMap<String, ArrayList<String>> map = pointsToMap.get(pPoint).get(col);

		map.clear();
		map.put("bot", new ArrayList<String>(Arrays.asList("bot")));

		return;
	}

	// This method checks if the COLUMN and a PROGRAM PONIT BOTh exists in DATA
	// CHECKED
	private void verifyPPAndCol(String pPoint, Integer col, String methodName) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> newPP = pointsToMap.get(pPoint);
		if (newPP == null)
			throw new NullPointerException(methodName + ": " + pPoint + " programPoint NOT present in pointsToMap");

		if (newPP.containsKey(col) == false)
			throw new NullPointerException(methodName + ": " + pPoint + " column NOT present in pointsToMap");

		return;
	}

	public void displayProgramPoint(String pPoint) {
		if (pointsToMap.get(pPoint) == null)
			throw new NullPointerException("In DISPLAYprogramPoint program point is null");

		HashMap<String, ArrayList<String>> hm_joined;
		HashMap<Integer, HashMap<String, ArrayList<String>>> map = pointsToMap.get(pPoint);

		System.out.println("BB" + pPoint.split("[.]")[1] + " -> BB" + pPoint.split("[.]")[2] + ":");

		hm_joined = new HashMap<String, ArrayList<String>>();

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : map.entrySet()) {
			Integer col = entry.getKey();
			if (!SetUpAnalysis.displayJoinedOutput)
				displayProgramPointUnderCol(pPoint, col);

			HashMap<String, ArrayList<String>> hm_col = retrieve(pPoint, col);
			for (Map.Entry<String, ArrayList<String>> e : hm_col.entrySet()) {
				String var = e.getKey();
				ArrayList<String> al_pointsto = e.getValue();
				for (String pointsto : al_pointsto) {
					ArrayList<String> al = hm_joined.get(var);
					if (al == null) {
						al = new ArrayList<String>();
						al.add(pointsto);

						hm_joined.put(var, al);
					} else {
						if (!al.contains(pointsto)) {
							al.add(pointsto);
						}
					}
				}
			}
		}
		if (SetUpAnalysis.displayJoinedOutput)
			displayMapJoin(hm_joined);
	}

	public void displayProgramPointUnderCol(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "displayProgramPointUnderCol");

		HashMap<String, ArrayList<String>> map = pointsToMap.get(pPoint).get(col);
		System.out.println("C" + col + ":");
		displayMap(map);
	}

	public void displayMap(HashMap<String, ArrayList<String>> map) {
		if (map == null)
			throw new NullPointerException("In DISPLAYMAP, map is null");

		if (map.containsKey("") && map.size() == 1) {
			System.out.println("{}\n");
			return;
		}

		if (map.containsKey("bot")) {
			System.out.println("bot\n");
			return;
		}

		String op = "";
		System.out.print("{");
		Boolean flag = true;
		for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			String var = entry.getKey();
			if (var.equals(""))
				continue;
			ArrayList<String> pointsto = entry.getValue();
			if (flag) {
				if (Character.isLetter(var.charAt(var.length() - 1)))
					op = "" + var + " -> " + "{";
				else
					op = "v" + var + " -> " + "{";
				flag = false;
			} else {
				if (Character.isLetter(var.charAt(var.length() - 1)))
					op = "\n " + var + " -> " + "{";
				else
					op = "\n v" + var + " -> " + "{";
			}
			for (String pt : pointsto) {
				op = op + pt + ", ";
			}
			op = op.substring(0, op.length() - 2);
			System.out.print(op + "}");
		}
		System.out.println("}\n");
	}

	public void displayMapJoin(HashMap<String, ArrayList<String>> map) {
		if (map == null)
			throw new NullPointerException("In DISPLAYMAP, map is null");

		if (map.containsKey("") && map.size() == 1) {
			System.out.println("{}\n");
			return;
		}

		if ((map.containsKey("bot") && map.size() == 1)
				|| (map.containsKey("bot") && map.size() == 2 && map.containsKey(""))) {
			System.out.println("bot\n");
			return;
		}

		String op = "";
		System.out.print("{");
		Boolean flag = true;
		for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			String var = entry.getKey();
			if (var.equals(""))
				continue;
			if (var.contains("bot"))
				continue;
			ArrayList<String> pointsto = entry.getValue();
			if (flag) {
				if (Character.isLetter(var.charAt(var.length() - 1)))
					op = "" + var + " -> " + "{";
				else
					op = "v" + var + " -> " + "{";
				flag = false;
			} else {
				if (Character.isLetter(var.charAt(var.length() - 1)))
					op = "\n " + var + " -> " + "{";
				else
					op = "\n v" + var + " -> " + "{";
			}
			for (String pt : pointsto) {
				op = op + pt + ", ";
			}
			op = op.substring(0, op.length() - 2);
			System.out.print(op + "}");
		}
		System.out.println("}\n");
	}

	// Returns TRUE if a particular column under a program point is BOT
	// CHECKED
	public boolean isBOT(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "isBOT");

		HashMap<String, ArrayList<String>> varMap = pointsToMap.get(pPoint).get(col);
		if (varMap.size() != 1)
			return false;

		ArrayList<String> pointsTo = varMap.get("bot");
		if (pointsTo == null)
			return false;
		return true;
	}

	// Returns TRUE if a particular column under a program point is NOT mapped
	// to anything
	public boolean isNullMap(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "isNullMap");

		HashMap<String, ArrayList<String>> map = pointsToMap.get(pPoint).get(col);
		if (map.size() != 1)
			return false;

		ArrayList<String> pointsTo = map.get("");
		if (pointsTo == null)
			return false;
		return true;
	}

	public int columnMapExists(String pPoint, HashMap<String, ArrayList<String>> map) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> columnMap = pointsToMap.get(pPoint);
		if (pointsToMap == null)
			throw new NullPointerException("columnMapExists: " + pPoint + " programPoint NOT present");

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : columnMap.entrySet()) {
			HashMap<String, ArrayList<String>> pointsToMap = entry.getValue();

			if (pointsToMap.equals(map))
				return entry.getKey();
		}
		return -1;
	}

	public int getNewColumnNum(String pPoint) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> columnMap = pointsToMap.get(pPoint);
		if (pointsToMap == null)
			throw new NullPointerException("getNewColumnNum: " + pPoint + " programPoint NOT present");

		return columnMap.size();
	}
}