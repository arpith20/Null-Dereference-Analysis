package PAVpointerAnalysisPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Data {

	private HashMap<String, ArrayList<String>> hm = new HashMap<String, ArrayList<String>>();
	private HashMap<String, HashMap<Integer, Boolean>> marked = new HashMap<String, HashMap<Integer, Boolean>>();
	private HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>> pp = new HashMap<String, HashMap<Integer, HashMap<String, ArrayList<String>>>>();

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

	// TODO: the following will most probably not work
	public boolean comparehash(HashMap<String, ArrayList<String>> hm1, HashMap<String, ArrayList<String>> hm2) {
		return hm1.equals(hm2);
	}

	public void sort(ArrayList<String> al) {
		if (al != null)
			Collections.sort(al);
	}

	// TODO untested
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

	// TODO untested
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

	// TODO untested
	public Boolean marked(String index, Integer col) {
		HashMap<Integer, Boolean> h = marked.get(index);
		if (h != null) {
			Boolean b = h.get(col);
			if (b != null)
				return b;
		}
		return true; // with an assumption that initially everything is marked
	}
}
