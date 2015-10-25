package PAVpointerAnalysisPackage;

import java.util.ArrayList;
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
				HashMap<String, ArrayList<String>> var_hash1 = al_col1.get(col);
				HashMap<String, ArrayList<String>> var_hash2 = entry.getValue();
				for (Map.Entry<String, ArrayList<String>> innerentry : var_hash2.entrySet()) {
					String var = innerentry.getKey();
					if (var_hash1.containsKey(var)) {
						ArrayList<String> al_pt1 = var_hash1.get(var);
						ArrayList<String> al_pt2 = innerentry.getValue();
						if (al_pt1.containsAll(al_pt2)) {
							return true;
						}
					}
				}
			}
		}
		return false;

	}

	//tested
	//returns true if pp1 dominates pp2, ie., all entries of pp2 is in pp1
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

	// returns the column numbers
	public ArrayList<Integer> contains(String pp1, String pp2, Integer col2) {
		ArrayList<Integer> a = new ArrayList<>();
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

	// most probable you'll use this
	// returns the column number in pp1 where hash of (pp2,col2) is equal
	public Integer equals(String pp1, String pp2, Integer col2) {
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
						if (equals(pp1, entry.getKey(), pp2, col2))
							return entry.getKey();
					}
				}
			}
		}
		return null;

	}

	public void add(String programpoint, Integer col, String variable, String pointsto) {
		HashMap<Integer, HashMap<String, ArrayList<String>>> al_col = pp.get(programpoint);
		if (al_col == null) {
			al_col = new HashMap<Integer, HashMap<String, ArrayList<String>>>();

			HashMap<String, ArrayList<String>> var_hash = new HashMap<String, ArrayList<String>>();

			ArrayList<String> al_pointsto = new ArrayList<String>();
			al_pointsto.add(pointsto);
			var_hash.put(variable, al_pointsto);
			al_col.put(col, var_hash);

			pp.put(programpoint, al_col);
		} else {
			HashMap<String, ArrayList<String>> var_hash = al_col.get(col);
			if (var_hash == null) {
				var_hash = new HashMap<String, ArrayList<String>>();

				ArrayList<String> al_pointsto = new ArrayList<String>();
				al_pointsto.add(pointsto);
				var_hash.put(variable, al_pointsto);
				al_col.put(col, var_hash);
			} else {
				ArrayList<String> al_pointsto = var_hash.get(variable);
				if (al_pointsto == null) {
					al_pointsto = new ArrayList<String>();
					al_pointsto.add(pointsto);

					var_hash.put(variable, al_pointsto);
				} else {
					if (!al_pointsto.contains(pointsto)) {
						al_pointsto.add(pointsto);
						sort(al_pointsto);
					}
				}
			}
		}
	}

	public void display() {
		String s_pointsto = "";
		if (pp == null)
			return;
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
				for (Map.Entry<String, ArrayList<String>> innerentry : var_hash.entrySet()) {
					String variable = innerentry.getKey();
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

	public void mark(String index, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(index);
		if (h == null) {
			h = new HashMap<Integer, Boolean>();
			h.put(col, true);
			marked.put(index, h);
		} else {
			h.put(col, true);
		}
	}

	public void unmark(String index, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(index);
		if (h == null) {
			h = new HashMap<Integer, Boolean>();
			h.put(col, false);
			marked.put(index, h);
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

	public ArrayList<String> retrive(String pp1, Integer col, String var) {
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

}
