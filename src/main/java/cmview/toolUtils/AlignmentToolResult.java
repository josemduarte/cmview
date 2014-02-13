package cmview.toolUtils;

import owl.core.sequence.alignment.MultipleSequenceAlignment;

public class AlignmentToolResult extends ToolResult {

	MultipleSequenceAlignment ali;
	double    score;
	
	public MultipleSequenceAlignment getAlignment() {
		return ali;
	}
	
	public void setAlignment(MultipleSequenceAlignment ali) {
		this.ali = ali;
	}
	
	public double getScore() {
		return score;
	}
	
	public void setScore(double score) {
		this.score = score;
	}
}
