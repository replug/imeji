package de.mpg.imeji.search.beans;

import de.mpg.imeji.search.Criterion;
import de.mpg.imeji.util.BeanHelper;

public class CriterionBean extends BeanHelper{
	
	private Criterion criterionVO;

	public Criterion getCriterionVO() {
		return criterionVO;
	}

	public void setCriterionVO(Criterion criterionVO) {
		this.criterionVO = criterionVO;
	}
}
