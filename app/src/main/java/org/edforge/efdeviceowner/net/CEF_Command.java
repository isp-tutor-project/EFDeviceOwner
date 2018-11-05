package org.edforge.efdeviceowner.net;

import org.edforge.util.CClassMap;
import org.edforge.util.ILoadableObject;
import org.edforge.util.IScope;
import org.edforge.util.JSON_Helper;
import org.json.JSONObject;

/**
 * Created by kevin on 10/20/2018.
 */

public class CEF_Command implements ILoadableObject {

    // json loadable
    public String command;
    public String to;
    public String from;
    public Boolean recurse;
    public long             size;
    public Boolean compress;
    public Boolean extract;


    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }
}
