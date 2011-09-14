package activityclassifier;

public class ExtractedFeature
{

    final String activityName;
    final float[] features;

    public ExtractedFeature( String activityName, float[] features ) {
        this.activityName = activityName;
        this.features = features;
    }
}
