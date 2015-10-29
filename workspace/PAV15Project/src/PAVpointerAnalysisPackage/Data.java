package PAVpointerAnalysisPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Data {

	private HashMap<String, HashMap<Integer, Boolean>> marked = new HashMap<String, HashMap<Integer, Boolean>>();
	private HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>> pp = new HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>>();

	// pp1 contains pp2, ie, pp2 is a subset of pp1
	public Boolean contains(String pp1, String pp2) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pp.get(pp2);
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
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pp.get(pp2);
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

	// do we need this?
	// untested.
	// returns the column numbers
	public ArrayList<Integer> contains(String pp1, String pp2, Integer col2) {
		ArrayList<Integer> a = new ArrayList<Integer>();
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pp.get(pp2);
		if (al_col1 == null && al_col2 == null)
			return null;
		if (al_col1 == null || al_col2 == null)
			return null;

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : al_col1.entrySet()) {
			HashMap<String, ArrayList<String>> var_hash1 = entry.getValue();
			HashMap<String, ArrayList<String>> var_hash2 = al_col2.get(col2);
			for (Map.Entry<String, ArrayList<String>> innerentry : var_hash2.entrySet()) {
				String var = innerentry.getKey();
				if (var_hash1.containsKey(var)) {
					ArrayList<String> al_pt1 = var_hash1.get(var);
					ArrayList<String> al_pt2 = innerentry.getValue();
					if (al_pt1.containsAll(al_pt2)) {
						a.add(entry.getKey());
					}
				}
			}
		}
		return a;

	}

	// DO NOT USE THIS DIRECTLY checks equality of two program points
	public Boolean equals(String pp1, String pp2) {
		if (contains(pp1, pp2) && contains(pp2, pp1))
			return true;
		return false;
	}

	// checks equality of two hash maps
	public Boolean equals(String pp1, Integer col1, String pp2, Integer col2) {
		if (contains(pp1, col1, pp2, col2) && contains(pp2, col2, pp1, col1))
			return true;
		return false;
	}

	// returns the column number in pp1 where hash of (pp2,col2) is equal
	public Integer equals(String pp1, String pp2, Integer col2) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pp.get(pp2);
		if (al_col1 == null && al_col2 == null)
			return null;
		if (al_col1 == null || al_col2 == null)
			return null;

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : al_col1.entrySet()) {
			Integer col1 = entry.getKey();
			if (contains(pp1, col1, pp2, col2) && contains(pp2, col2, pp1, col1))
				return col1;
		}
		return null;
	}

	// Function to add a mapping from VARIABLE to POINTSTO
	// Removes BOT if already present
	// STRONG UPDATE
	// Returns TRUE if anything is updated in the map. Else, FALSE
	public Boolean add(String pPoint, Integer col, String var, String pointsTo) {
		verifyPPAndCol(pPoint, col, "add");

		HashMap<String, ArrayList<String>> var_hash = pp.get(pPoint).get(col);

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

		HashMap<String, ArrayList<String>> originalMap = pp.get(pPoint).get(col);
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
		if (pp == null)
			throw new NullPointerException("How is this even possible?");
		for (Map.Entry<String, HashMap<Integer, HashMap<String, ArrayList<String>>>> entry : pp.entrySet()) {
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

	public void sort(ArrayList<String> al) {
		if (al != null)
			Collections.sort(al);
	}

	// This will mark the column COL under the program point PP
	public void mark(String pp, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(pp);
		if (h == null) {
			throw new NullPointerException(
					"mark: " + pp + " " + col + " Program Point doesn't exist while trying to mark");
		}
		h.put(col, true);
	}

	// This will un-mark the column COL under the program point PP
	public void unmark(String pp, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(pp);
		if (h == null) {
			throw new NullPointerException(
					"unmark: " + pp + " " + col + " Program Point doesn't exist while trying to unmark");
		}
		h.put(col, false);
	}

	// This will return TRUE if the column COL is maked under the program point
	// PP
	// With an assumption that initially everything is marked
	public Boolean marked(String pp, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(pp);
		if (h == null) {
			throw new NullPointerException(
					"marked: " + pp + " " + col + " Program Point doesn't exist while trying check if marked");
		}
		return h.get(col);
	}

	// Return TRUE only if all the columns under a program point PP is unmarked
	public Boolean checkAllColumnsUnmarked(String pp) {
		HashMap<Integer, Boolean> h = marked.get(pp);
		if (h == null) {
			throw new NullPointerException("checkAllColumnsUnmarked: " + pp + " Program Point doesn't exist");
		}
		for (Map.Entry<Integer, Boolean> entry : h.entrySet()) {
			Boolean mark = entry.getValue();
			if (mark)
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

//		if (pPoint.equals("phiTest.11.16")) {
//			if (col == 0) {
//				System.out.println("Map at 11.16 is \n");
//
//				System.out.println(map);
//				System.out.println("PP is\n");
//				displayProgramPointUnderCol(pPoint, col);
//			}
//		}

		Boolean flag = false;
		if ( map.containsKey("bot")) {

			// Check if the COL under PPOINT is NULL
			if (isNullMap(pPoint, col) || pp.get(pPoint).get(col).size() == 0 ) {
				setToBOT(pPoint, col);
				mark(pPoint, col);
				return true;
			}
			else if ( !isBOT(pPoint,col))
				return false ;
		}

		// Remove BOT entry from the next program point
		// Also remove "" mapping if it is there
		HashMap<String, ArrayList<String>> oldMap = retrieve(pPoint, col);
//		if (oldMap.containsKey("bot"))
//			remove(pPoint, col, "bot");

		for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			String var = entry.getKey();
			ArrayList<String> pointsto = entry.getValue();

			for (String pt : pointsto) {
				if ( pt.equals(""))
					continue ;
				if (add(pPoint, col, var, pt)) {
					mark(pPoint, col);
					flag = true;
				}
			}
		}
		
//		if (pPoint.equals("phiTest.16.17")) {
//			if (col == 0) {
////				System.out.println("Map at 16.17 is \n");
////
////				System.out.println(map);
////				System.out.println("\n");
//				displayProgramPointUnderCol(pPoint, col);
//			}
//		}
		return flag;
	}

	// pp1 = pp1 union pp2
	public void join(String pp1, Integer col1, String pp2, Integer col2) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col2 = pp.get(pp2);
		if (al_col2 != null) {
			HashMap<String, ArrayList<String>> h = al_col2.get(col2);
			if (h != null) {
				for (Map.Entry<String, ArrayList<String>> entry : h.entrySet()) {
					String var = entry.getKey();
					ArrayList<String> pointsto = entry.getValue();
					for (String pt : pointsto) {
						add(pp1, col1, var, pt);
					}
				}
			}
		}
	}

	// pp1 = pp2 union pp3
	// previous values at pp1, if any are removed
	public void join(String pp1, Integer col1, String pp2, Integer col2, String pp3, Integer col3) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		if (al_col1 != null) {
			al_col1.clear();
		}
		join(pp1, col1, pp2, col2);
		join(pp1, col1, pp3, col3);
	}

	public void remove(String pp1, Integer col, String var, String pttoremove) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		if (al_col1 != null) {
			HashMap<String, ArrayList<String>> h = al_col1.get(col);
			if (h != null) {
				ArrayList<String> a = h.get(var);
				a.remove(pttoremove);
			}
		}
	}

	public void remove(String pp1, Integer col, String var) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
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

		if ( pp.get(pPoint).get(col) == null )
			System.out.println("HashMap is null");
		// Check if the pointsTo set of VAR is empty
		ArrayList<String> map = pp.get(pPoint).get(col).get(var);
		if (map == null)
			return null;
		else
			return new ArrayList<String>(pp.get(pPoint).get(col).get(var));
	}

	// Retrieves the mappings of all the variables under the column COL at the
	// program point PPOINT
	// This is a COPY of the HashMap
	public HashMap<String, ArrayList<String>> retrieve(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "retrieve HashMap");

		return new HashMap<String, ArrayList<String>>(pp.get(pPoint).get(col));
	}

	// Retrieves the mappings of all the columns under the program point pPoint
	// This is a COPY of the HashMap
	public HashMap<Integer, HashMap<String, ArrayList<String>>> retrieve(String pPoint) {
		return new HashMap<Integer, HashMap<String, ArrayList<String>>>(pp.get(pPoint));
	}

	public HashMap<Integer, Boolean> getColumnMarkings(String pPoint) {
		if (marked.get(pPoint) != null)
			return marked.get(pPoint);
		else
			return null;
	}

	public void openColumn(String pPoint, Integer col) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> hm = pp.get(pPoint);
		if (hm != null) {
			HashMap<String, ArrayList<String>> hMap = new HashMap<String, ArrayList<String>>();
			hm.put(col, hMap);
		} else {
			throw new NullPointerException("openColumn: " + pPoint + " " + col + " programPoint not present");
		}

		// Open a column in the MARKED data structure as well and MARK it
		HashMap<Integer, Boolean> markings = marked.get(pPoint);
		markings.put(col, true);
	}

	public void addProgramPoint(String pPoint) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> newPP = pp.get(pPoint);
		if (newPP == null) {
			newPP = new HashMap<Integer, HashMap<String, ArrayList<String>>>();
			pp.put(pPoint, newPP);

			// Create a HashMap for this program point in MARKED as well
			HashMap<Integer, Boolean> markings = new HashMap<Integer, Boolean>();
			marked.put(pPoint, markings);
		} else {
			throw new NullPointerException("addProgramPoint: " + pPoint + "programPoint already present");
		}
	}

	// Method to make a column at a particular program point to be mapped to BOT
	public void setToBOT(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "setToBot");

		HashMap<String, ArrayList<String>> map = pp.get(pPoint).get(col);

		map.clear();
		map.put("bot", new ArrayList<String>(Arrays.asList("bot")));

		return;
	}

	// This method checks if the COLUMN and a PROGRAM PONIT BOTh exists in DATA
	private void verifyPPAndCol(String pPoint, Integer col, String methodName) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> newPP = pp.get(pPoint);
		if (newPP == null)
			throw new NullPointerException(methodName + ": " + pPoint + " programPoint NOT present");

		if (!newPP.containsKey(col))
			throw new NullPointerException(methodName + ": " + pPoint + " column NOT present");

		return;
	}

	public void displayProgramPoint(String pPoint) {
		if (pp.get(pPoint) == null)
			throw new NullPointerException("In DISPLAYprogramPoint program point is null");

		HashMap<Integer, HashMap<String, ArrayList<String>>> map = pp.get(pPoint);

		System.out.println("BB" + pPoint.split("[.]")[1] + " -> BB" + pPoint.split("[.]")[2] + ":");
		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : map.entrySet()) {
			Integer col = entry.getKey();
			displayProgramPointUnderCol(pPoint, col);
		}
	}

	public void displayProgramPointUnderCol(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "displayProgramPointUnderCol");

		HashMap<String, ArrayList<String>> map = pp.get(pPoint).get(col);
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
				op = "v" + var + " -> " + "{";
				flag = false;
			} else
				op = "\n v" + var + " -> " + "{";
			for (String pt : pointsto) {
				op = op + pt + ", ";
			}
			op = op.substring(0, op.length() - 2);
			System.out.print(op + "}");
		}
		System.out.println("}\n");
	}

	// Returns TRUE if a particular column under a program point is BOT
	public boolean isBOT(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "isBOT");

		HashMap<String, ArrayList<String>> map = pp.get(pPoint).get(col);
		if (map.size() != 1)
			return false;

		ArrayList<String> pointsTo = map.get("bot");
		if (pointsTo == null)
			return false;
		return true;
	}

	// Returns TRUE if a particular column under a program point is NOT mapped
	// to anything
	public boolean isNullMap(String pPoint, Integer col) {
		verifyPPAndCol(pPoint, col, "isNullMap");

		HashMap<String, ArrayList<String>> map = pp.get(pPoint).get(col);
		if (map.size() != 1)
			return false;

		ArrayList<String> pointsTo = map.get("");
		if (pointsTo == null)
			return false;
		return true;
	}

	public int columnMapExists(String pPoint, HashMap<String, ArrayList<String>> map) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> columnMap = pp.get(pPoint);
		if (pp == null)
			throw new NullPointerException("columnMapExists: " + pPoint + " programPoint NOT present");

		for (Map.Entry<Integer, HashMap<String, ArrayList<String>>> entry : columnMap.entrySet()) {
			HashMap<String, ArrayList<String>> pointsToMap = entry.getValue();

			if (pointsToMap.equals(map))
				return entry.getKey();
		}
		return -1;
	}

	public int getNewColumnNum(String pPoint) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> columnMap = pp.get(pPoint);
		if (pp == null)
			throw new NullPointerException("getNewColumnNum: " + pPoint + " programPoint NOT present");

		return columnMap.size();
	}
}