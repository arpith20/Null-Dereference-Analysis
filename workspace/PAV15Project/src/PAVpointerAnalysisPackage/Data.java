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

	public Boolean add(String programpoint, Integer col, String variable, String pointsto) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col = pp.get(programpoint);
		if (al_col == null) {
			// al_col = new HashMap<Integer, HashMap<String,
			// ArrayList<String>>>();
			//
			// HashMap<String, ArrayList<String>> var_hash = new HashMap<String,
			// ArrayList<String>>();
			//
			// ArrayList<String> al_pointsto = new ArrayList<String>();
			// al_pointsto.add(pointsto);
			// var_hash.put(variable, al_pointsto);
			// al_col.put(col, var_hash);
			//
			// pp.put(programpoint, al_col);
			throw new NullPointerException(
					"Exception in add. Program point: " + programpoint + " is not mapped to anything");
		} else {
			HashMap<String, ArrayList<String>> var_hash = al_col.get(col);
			if (var_hash == null) {
				// var_hash = new HashMap<String, ArrayList<String>>();
				//
				// ArrayList<String> al_pointsto = new ArrayList<String>();
				// al_pointsto.add(pointsto);
				// var_hash.put(variable, al_pointsto);
				// al_col.put(col, var_hash);
				throw new NullPointerException("Exception in add. Column: " + col + " at program point " + programpoint
						+ " is not mapped to anything");
			} else {
				remove(programpoint, col, "bot");
				ArrayList<String> al_pointsto = var_hash.get(variable);
				if (al_pointsto == null) {
					al_pointsto = new ArrayList<String>();
					al_pointsto.add(pointsto);

					var_hash.put(variable, al_pointsto);
					return true;
				} else {
					if (!al_pointsto.contains(pointsto)) {
						al_pointsto.add(pointsto);
						sort(al_pointsto);
						return true;
					} else
						return false;

				}
			}
		}
	}

	public void display() {
		System.out.println("inside display");
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

	public void mark(String pp, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(pp);
		if (h == null) {
			throw new NullPointerException(
					"mark: " + pp + " " + col + " Program Point doesn't exist while trying to mark");
		} else {
			h.put(col, true);
		}
	}

	public void unmark(String index, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(index);
		if (h == null) {
			throw new NullPointerException(
					"unmark: " + index + " " + col + " Program Point doesn't exist while trying to mark");
		} else {
			h.put(col, false);
		}
	}

	public Boolean marked(String index, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(index);
		if (h != null) {
			Boolean b = h.get(col);
			if (b != null)
				return b;
		}
		return true; // with an assumption that initially everything is marked
	}

	public Boolean checkAllColumnsUnmarked(String program_point) {
		HashMap<Integer, Boolean> h = marked.get(program_point);
		// if (h == null)
		// return true;
		for (Map.Entry<Integer, Boolean> entry : h.entrySet()) {
			Integer column = entry.getKey();
			Boolean point = entry.getValue();
			if (point)
				return false;
		}
		return true;
	}

	public Boolean propagate(String ppoint, Integer col, HashMap<String, ArrayList<String>> h) {

		Boolean flag = false;
		if (h != null) {
			if (h.containsKey("bot")) {
				return true; // TODO
			}
			for (Map.Entry<String, ArrayList<String>> entry : h.entrySet()) {
				String var = entry.getKey();
				ArrayList<String> pointsto = entry.getValue();
				for (String pt : pointsto) {
					if (add(ppoint, col, var, pt)) {
						mark(ppoint, col);
						flag = true;
					}
				}
			}
			return flag;
		} else {
			throw new NullPointerException("H is null here");
		}
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

	public ArrayList<String> retrieve(String pp1, Integer col, String var) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		if (al_col1 != null) {
			HashMap<String, ArrayList<String>> h = al_col1.get(col);
			if (h != null) {
				ArrayList<String> a = h.get(var);
				// for(String s: a)
				// System.out.println(s);
				return a;
			}
		}
		return null;
	}

	public HashMap<String, ArrayList<String>> retrieve(String pp1, Integer col) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col1 = pp.get(pp1);
		if (al_col1 != null) {
			HashMap<String, ArrayList<String>> h = al_col1.get(col);
			if (h != null) {
				return new HashMap<String, ArrayList<String>>(h);
			}
		}
		return null;
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
}