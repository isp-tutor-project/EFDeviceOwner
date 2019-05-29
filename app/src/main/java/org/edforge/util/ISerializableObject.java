package org.edforge.util;

import org.json.JSONObject;


public interface ISerializableObject {

    public void saveJSON(JSON_Util jsonWriter);
    public void loadJSON(JSONObject jsonObj, IScope scope);
}
