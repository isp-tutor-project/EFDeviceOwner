package org.edforge.efdeviceowner;


import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

/**
 * Created by kevin on 5/8/2019.
 */

public class InstructionConfig implements ISerializableObject {

    public String comment;
    public String defInstr;
    public String[] options;

    @Override
    public void saveJSON(JSON_Util jsonWriter) {

        jsonWriter.addElement("comment",       comment);
        jsonWriter.addElement("defInstr", defInstr);
        jsonWriter.addStringArray("options", options);
    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }}
