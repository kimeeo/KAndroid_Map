package com.kimeeo.kAndroid.mapDemo;


import com.google.android.gms.maps.model.Marker;
import com.kimeeo.kAndroid.listViews.dataProvider.DataModel;
import com.kimeeo.kAndroid.listViews.dataProvider.IParseableObject;
import com.kimeeo.kAndroid.map.IPOI;

import java.util.List;

/**
 * Created by bhavinpadhiyar on 12/26/15.
 */
public class SamplePOIModel extends Object implements IParseableObject,IPOI
{
    public String name;
    public String title="data";
    public String subTitle;
    public String icon;
    public String id;
    public String image;
    public String details;
    public List<SamplePOIModel> children;
    private Marker marker;
    private String snippet="data";
    public Double latitude;
    public Double longitude;
    public Double altitude;

    public void dataLoaded(DataModel FullData)
    {

    }

    public Double getLatitude()
    {
        return latitude;
    }
    public Double getLongitude()
    {
        return longitude;
    }
    public Double getAltitude()
    {
        return altitude;
    }
    public String getTitle()
    {
        return title;
    }
    public String getSnippet()
    {
        return snippet;
    }
    public void setSnippet(String val)
    {
        this.snippet=val;
    }
    public String getId()
    {
        return id;
    }

    public Marker getMarker()
    {
        return marker;
    }
    public void setMarker(Marker marker)
    {
        marker.setSnippet(getSnippet());
        this.marker=marker;
    }

}
