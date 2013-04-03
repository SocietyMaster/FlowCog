package soot.jimple.infoflow.android.data;

import java.util.Comparator;

public class AndroidMethodCategoryComparator implements Comparator<AndroidMethod>{

	@Override
	public int compare(AndroidMethod m1, AndroidMethod m2) {
		if(m1.getCategory() == null && m2.getCategory() == null)
			return 0;
		else if(m1.getCategory() == null)
			return 1;
		else if(m2.getCategory() == null)
			return -1;
		else
			return m1.getCategory().compareTo(m2.getCategory());
	}

}
