/*
 *
 *     Copyright(c) 2016 Kevin Willows All Rights Reserved
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package org.edforge.util;

import android.os.Environment;
import android.util.JsonWriter;
import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 * @author kevin
 */
public class JSON_Util {

    private StringWriter        outString;
    private JsonWriter          writer;

    static private String       dataPath;
    static private Boolean      dataMode;

    static private FileWriter   fileWriter;
    static private boolean      fileWriterValid = false;


    final private String TAG = "JSON_Util";


    public JSON_Util()  {

        outString = new StringWriter();
        writer    = new JsonWriter(outString);

        try {
            writer.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public JSON_Util(ISerializableObject obj)  {

        outString = new StringWriter();
        writer    = new JsonWriter(outString);

        try {
            writer.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        obj.saveJSON(this);
    }


    public void write(ISerializableObject obj, String file_Path, boolean append_file)  {

        outString = new StringWriter();
        writer    = new JsonWriter(outString);

        try {
            writer.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        obj.saveJSON(this);

        dataPath  = file_Path;
        dataMode  = append_file;

        writeResult();
    }


    public void addObject(ISerializableObject obj)  {

        try {
            writer.beginObject();

            obj.saveJSON(this);

            writer.endObject();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Object add failed: " +  e);
        }
    }


    public void addObject(String key, String value)  {

        try {
            writer.beginObject();

            addElement(key, value);

            writer.endObject();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Object add failed: " +  e);
        }
    }


    public void startHashMap(String name)  {

        try {
            writer.name(name);
            writer.beginObject();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json HashMap start failed: " +  e);
        }
    }

    public void endHashMap()  {

        try {
            writer.endObject();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json HashMap end failed: " +  e);
        }
    }


    public void addHashMap(String HashName, HashMap map) {

        startHashMap(HashName);

        Iterator<?> tObjects = map.entrySet().iterator();

        while(tObjects.hasNext() ) {

            Map.Entry pattern = (Map.Entry) tObjects.next();

            ISerializableObject mapObject = ((ISerializableObject)pattern.getValue());

            addElement((String)pattern.getKey(), mapObject);
        }

        endHashMap();
    }


    public void startArray(String name)  {

        try {
            writer.name(name);
            writer.beginArray();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Array start failed: " +  e);
        }
    }

    public void addArray(String stringval)  {

        try {
            writer.value(stringval);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Array add failed: " +  e);
        }
    }

    public void addArray(int intval)  {

        try {
            writer.value(intval);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Array add failed: " +  e);
        }
    }

    public void addArray(ISerializableObject objval)  {

        try {
            addObject(objval);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Array add failed: " +  e);
        }
    }

    public void addStringArray(String name, String[] strArray) {

        startArray(name);

        for(String item : strArray) {

            addArray(item);
        }
        endArray();
    }


    public void addIntArray(String name, int[] intArray) {

        startArray(name);

        for(int item : intArray) {

            addArray(item);
        }
        endArray();
    }


    public void addHexArray(String name, int[] intArray) {

        startArray(name);

        for(int item : intArray) {

            addArray(("0x" + Integer.toHexString(item)).toUpperCase());
        }
        endArray();
    }


    public void addValueArray(String name, String[] objArray) {

        startArray(name);

        for(String item : objArray) {

            try {
                writer.value(item);

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
        endArray();
    }


    public void addObjectArray(String name, ISerializableObject[] objArray) {

        startArray(name);

        for(ISerializableObject item : objArray) {

            addArray(item);
        }
        endArray();
    }


    public void endArray()  {

        try {
            writer.endArray();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Array end failed: " +  e);
        }
    }

    public String getValue()  {

        return outString.toString();
    }

    public void addElement(String key, String value)  {

        try {
            writer.name(key);
            writer.value(value);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Element add String failed: " +  e);
        }
    }

    public void addElement(String key, int value)  {

        try {
            writer.name(key);
            writer.value(value);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Element add int failed: " +  e);
        }
    }

    public void addElement(String key, long value)  {

        try {
            writer.name(key);
            writer.value(value);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Element add int failed: " +  e);
        }
    }

    public void addElement(String key, boolean value)  {

        try {
            writer.name(key);
            writer.value(value);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Element add boolean failed: " +  e);
        }
    }

    public void addElement(String key, ISerializableObject obj)  {

        try {
            writer.name(key);
            addObject(obj);
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json Element add obj failed: " +  e);
        }
    }

    public String close() {

        try {
            writer.endObject();
        }
        catch(Exception e) {
            System.out.println("JSON_Util - Json close failed: " +  e);
        }

        return outString.toString();
    }

    public String prettyClose() {

//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        JsonParser jp = new JsonParser();
//        JsonElement je = jp.parse(close());
//        String prettyJsonString = gson.saveJSON(je);

//        return prettyJsonString;
        return "";
    }


    /**
     *
     */
    public void writeResult() {

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            try {
                fileWriter = new FileWriter(dataPath, dataMode);

                fileWriter.write(close());
                fileWriter.flush();
                fileWriter.close();

            } catch (Exception e) {
                Log.e(TAG, "file creation Failed: " + e);
            }
        }
    }

}
