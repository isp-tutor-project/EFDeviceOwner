package org.edforge.util;

import org.json.JSONObject;


public interface ILoadableObject {
    public void loadJSON(JSONObject jsonObj, IScope scope);
}
