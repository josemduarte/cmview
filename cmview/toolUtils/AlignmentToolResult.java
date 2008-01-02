package cmview.toolUtils;

import proteinstructure.Alignment;

public class AlignmentToolResult extends ToolResult {

	Alignment ali;
	double    score;
	
	public Alignment getAlignment() {
		return ali;
	}
	
	public void setAlignment(Alignment ali) {
		this.ali = ali;
	}
	
	public double getScore() {
		return score;
	}
	
	public void setScore(double score) {
		this.score = score;
	}
}
