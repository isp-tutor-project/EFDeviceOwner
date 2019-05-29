package org.edforge.efdeviceowner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.edforge.util.TCONST;
import org.json.JSONObject;

import static org.edforge.efdeviceowner.OwnerActivity.ASSET_FOLDER;
import static org.edforge.efdeviceowner.OwnerActivity.DATA_PATH;
import static org.edforge.util.TCONST.ANDROID_BREAK_OUT;
import static org.edforge.util.TCONST.GO_HOME;
import static org.edforge.util.TCONST.REBOOT_DEVICE;
import static org.edforge.util.TCONST.WIPE_DEVICE;

/**
 * Created by kevin on 5/7/2019.
 */

public class DefInstructionView extends LinearLayout {

    private Context mContext;
    private LocalBroadcastManager bManager;

    private Button          bHome;
    private TextView        SdefaultInstruction;
    private LinearLayout    SbuttonArray;
    private TextView        Spreview;
    private int             defInstNdx;

    private InstructionConfig mInstructionConfig;


    final private String       TAG = "DefInstructionView";


    public DefInstructionView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public DefInstructionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public DefInstructionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void init(Context context, AttributeSet attrs) {

        mContext = context;

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(getContext());
    }


    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        SdefaultInstruction  = (TextView) findViewById(R.id.defaultInstruction);
        SbuttonArray         = (LinearLayout) findViewById(R.id.buttonArray);
        Spreview             = (TextView) findViewById(R.id.preview);

        bHome = (Button)   findViewById(R.id.bHome);
        bHome.setOnClickListener(clickListener);
    }


    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }


    public void configView() {

        loadInstructionConfig();

        SdefaultInstruction.setText(mInstructionConfig.defInstr);

        SbuttonArray.removeAllViews();

        addOptionButtons();

    }


    private void addOptionButtons() {

        int btnColor;

        for(int i1 = 0 ; i1 < mInstructionConfig.options.length ; i1++) {
            //set the properties for button
            Button btnTag = new Button(mContext);

            LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0,0,0,5);
            btnTag.setLayoutParams(params);
            btnTag.setText(mInstructionConfig.options[i1]);
            btnTag.setId(i1);

            if(mInstructionConfig.options[i1].equals(mInstructionConfig.defInstr)) {
                btnColor = Color.parseColor("#FF0000ff");
                defInstNdx = i1;
            }
            else btnColor = Color.parseColor("#FF000099");

            btnTag.setBackgroundColor(btnColor);
            btnTag.setTextColor(Color.parseColor("#FFFFFFFF"));

            //add button to the layout
            SbuttonArray.addView(btnTag);

            btnTag.setOnClickListener(clickListener);
        }
    }


    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            int vid = v.getId();

            if (vid == R.id.bHome) {

                saveInstructionConfig();
                broadcast(GO_HOME);

            } else {

                Button defButton = SbuttonArray.findViewById(defInstNdx);
                defButton.setBackgroundColor(Color.parseColor("#FF000099"));

                mInstructionConfig.defInstr = mInstructionConfig.options[vid];
                SdefaultInstruction.setText(mInstructionConfig.defInstr);

                defInstNdx = vid;
                defButton = SbuttonArray.findViewById(defInstNdx);
                defButton.setBackgroundColor(Color.parseColor("#FF0000FF"));
            }
        }
    };


    private void loadInstructionConfig() {

        // Load the user data file
        //
        mInstructionConfig = new InstructionConfig();
        String jsonData  = JSON_Helper.cacheDataByName(ASSET_FOLDER + TCONST.INSTR_CONFIG);

        try {
            if(!jsonData.isEmpty())
                mInstructionConfig.loadJSON(new JSONObject(jsonData), null);

        } catch (Exception e) {

            // TODO: Manage Exceptions
            Log.e(TAG, "UserData Parse Error: " + e);
        }
    }


    private void saveInstructionConfig() {

        JSON_Util jsonWriter = new JSON_Util();

        jsonWriter.write(mInstructionConfig, ASSET_FOLDER + TCONST.INSTR_CONFIG, false);
    }

}
