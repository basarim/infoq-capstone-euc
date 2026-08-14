package com.euc.grantfitassessment;

/**
 * A prompt-variant FitReasoner for the drift experiment (docs/proposal.md
 * Section 7, step 3: "edit the prompt's alignment instructions"). Same
 * model, same EUC, same everything else as LlmFitReasoner — only the
 * alignment-instructions paragraph changes, deliberately loosening the
 * "cite evidence or say so" constraint so a real run can test whether
 * evaluation's evidenceGrounding criterion catches the resulting drop in
 * evidence discipline.
 */
public class AlternateAlignmentPromptReasoner extends LlmFitReasoner {

    public AlternateAlignmentPromptReasoner(String modelName) {
        super(modelName);
    }

    @Override
    protected String alignmentInstructions() {
        return "Assess mission and program alignment. Give your best judgment of fit "
                + "even if the organization's profile doesn't fully spell out the connection.";
    }
}
