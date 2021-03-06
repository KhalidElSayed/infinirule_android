package nz.gen.geek_central.infinirule;
/*
    Infinirule--the infinitely stretchable and scrollable slide rule, mainline.

    Copyright 2011, 2012 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    This program is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see
    <http://www.gnu.org/licenses/>.
*/

public class Main
    extends android.app.Activity
    implements SlideView.ContextMenuAction, SlideView.ScaleNameClickAction
  {

    public static byte[] ReadAll
      (
        java.io.InputStream From
      )
      /* reads all available data from From. */
    throws java.io.IOException
      {
        java.io.ByteArrayOutputStream Result = new java.io.ByteArrayOutputStream();
        final byte[] Buf = new byte[256]; /* just to reduce number of I/O operations */
        for (;;)
          {
            final int BytesRead = From.read(Buf);
            if (BytesRead < 0)
                break;
            Result.write(Buf, 0, BytesRead);
          } /*for*/
        return
            Result.toByteArray();
      } /*ReadAll*/

    public void ShowHelp
      (
        String Path,
        String[] FormatArgs
      )
      /* launches the Help activity, displaying the page in my resources with
        the specified Path. */
      {
        final android.content.Intent LaunchHelp =
            new android.content.Intent(android.content.Intent.ACTION_VIEW);
      /* must always load the page contents, can no longer pass a file:///android_asset/
        URL with Android 4.0. */
        byte[] HelpRaw;
          {
            java.io.InputStream ReadHelp;
            try
              {
                ReadHelp = getAssets().open(Path);
                HelpRaw = ReadAll(ReadHelp);
              }
            catch (java.io.IOException Failed)
              {
                throw new RuntimeException("can't read help page: " + Failed);
              } /*try*/
            try
              {
                ReadHelp.close();
              }
            catch (java.io.IOException WhoCares)
              {
              /* I mean, really? */
              } /*try*/
          }
        LaunchHelp.putExtra
          (
            nz.gen.geek_central.infinirule.Help.ContentID,
            FormatArgs != null ?
                String.format(Global.StdLocale, new String(HelpRaw), FormatArgs)
                    .getBytes()
            :
                HelpRaw
          );
        LaunchHelp.setClass(this, Help.class);
        startActivity(LaunchHelp);
      } /*ShowHelp*/

    private java.util.Map<android.view.MenuItem, Runnable> OptionsMenu;
    private java.util.Map<android.view.MenuItem, Runnable> ContextMenu;

    interface RequestResponseAction /* response to an activity result */
      {
        public void Run
          (
            int ResultCode,
            android.content.Intent Data
          );
      } /*RequestResponseAction*/

    private java.util.Map<Integer, RequestResponseAction> ActivityResultActions;

  /* request codes */
    private static final int SetFirstScaleRequest = 1;
      /* arbitrary starting point for contiguous range mapping to scale indexes */
    private static final int SetLastPlusOneScaleRequest = SetFirstScaleRequest + SCALE.NR;
      /* exclusive end of contiguous range */

    private SlideView Slide;
    private android.text.ClipboardManager Clipboard;

    @Override
    public void onCreate
      (
        android.os.Bundle SavedInstanceState
      )
      {
        super.onCreate(SavedInstanceState);
        getWindow().requestFeature(android.view.Window.FEATURE_CUSTOM_TITLE);
        Clipboard = (android.text.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        BuildActivityResultActions();
        Global.MainMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(Global.MainMetrics);
        Scales.LoadParams(getResources());
        setContentView(R.layout.main);
        Slide = (SlideView)findViewById(R.id.slide_view);
        Slide.SetContextMenuAction(this);
        Slide.SetScaleNameClickAction(this);
      } /*onCreate*/

    @Override
    public void onPostCreate
      (
        android.os.Bundle SavedInstanceState
      )
      {
        super.onPostCreate(SavedInstanceState);
        getWindow().setFeatureInt
          (
            android.view.Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_bar
          );
        ((android.widget.Button)findViewById(R.id.action_help)).setOnClickListener
          (
            new android.view.View.OnClickListener()
              {
                public void onClick
                  (
                    android.view.View ButtonView
                  )
                  {
                    ShowHelp("help/index.html", null);
                  } /*onClick*/
              } /*OnClickListener*/
          );
      } /*onPostCreate*/

    @Override
    public boolean onCreateOptionsMenu
      (
        android.view.Menu TheMenu
      )
      {
        OptionsMenu = new java.util.HashMap<android.view.MenuItem, Runnable>();
        OptionsMenu.put
          (
            TheMenu.add(R.string.show_help),
            new Runnable()
              {
                public void run()
                  {
                    ShowHelp("help/index.html", null);
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.reset),
            new Runnable()
              {
                public void run()
                  {
                    Slide.Reset(true);
                  } /*run*/
              } /*Runnable*/
          );
        OptionsMenu.put
          (
            TheMenu.add(R.string.about_me),
            new Runnable()
              {
                public void run()
                  {
                    String VersionName;
                    try
                      {
                        VersionName =
                            getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                      }
                    catch (android.content.pm.PackageManager.NameNotFoundException CantFindMe)
                      {
                        VersionName = "CANTFINDME"; /*!*/
                      } /*catch*/
                    ShowHelp("help/about.html", new String[] {VersionName});
                  } /*run*/
              } /*Runnable*/
          );
        return
            true;
      } /*onCreateOptionsMenu*/

    void BuildActivityResultActions()
      {
        ActivityResultActions = new java.util.HashMap<Integer, RequestResponseAction>();
        for (int WhichScale = 0; WhichScale < SCALE.NR; ++WhichScale)
          {
            final int LocalWhichScale = WhichScale;
            ActivityResultActions.put
              (
                SetFirstScaleRequest + WhichScale,
                new RequestResponseAction()
                  {
                    public void Run
                      (
                        int ResultCode,
                        android.content.Intent Data
                      )
                      {
                        Slide.SetScale(Data.getStringExtra(ScalePicker.NameID), LocalWhichScale);
                      } /*Run*/
                  } /*RequestResponseAction*/
              );
          } /*for*/
      } /*BuildActivityResultActions*/

    public void CreateContextMenu
      (
        android.view.ContextMenu TheMenu,
        SlideView.ContextMenuTypes MenuType
      )
      {
        ContextMenu = new java.util.HashMap<android.view.MenuItem, Runnable>();
        switch (MenuType)
          {
        case Cursor:
            for (boolean Pasting = false;;)
              {
                for (int WhichScale = 0; WhichScale < SCALE.NR; ++WhichScale)
                  {
                    final int LocalWhichScale = WhichScale;
                    ContextMenu.put
                      (
                        TheMenu.add
                          (
                            String.format
                              (
                                getString(Pasting ? R.string.paste_prompt : R.string.copy_prompt),
                                getString(Global.ScaleNameID(WhichScale))
                              )
                          ),
                        Pasting ?
                            new Runnable()
                              {
                                public void run()
                                  {
                                    final CharSequence NumString = Clipboard.getText();
                                    if (NumString != null)
                                      {
                                        final Scales.Scale TheScale = Slide.GetScale(LocalWhichScale);
                                        try
                                          {
                                            final double Value = Double.parseDouble(NumString.toString());
                                            double ScalePos = TheScale.PosAt(Value);
                                            if
                                              (
                                                    TheScale.Wrap()
                                                ||
                                                    ScalePos >= 0.0 && ScalePos < 1.0
                                              )
                                              {
                                                Slide.SetCursorPos
                                                  (
                                                    /*ByScale =*/ LocalWhichScale,
                                                    /*NewPos =*/ ScalePos - Math.floor(ScalePos),
                                                    /*Animate =*/ true
                                                  );
                                              }
                                            else
                                              {
                                                android.widget.Toast.makeText
                                                  (
                                                    /*context =*/ Main.this,
                                                    /*text =*/
                                                        String.format
                                                          (
                                                            getString(R.string.paste_range),
                                                            Global.FormatNumber(Value),
                                                            getString(Global.ScaleNameID(LocalWhichScale)),
                                                            Global.FormatNumber(TheScale.ValueAt(0.0)),
                                                            Global.FormatNumber(TheScale.ValueAt(1.0))
                                                          ),
                                                    /*duration =*/ android.widget.Toast.LENGTH_SHORT
                                                  ).show();
                                              } /*if*/
                                          }
                                        catch (NumberFormatException BadNum)
                                          {
                                            android.widget.Toast.makeText
                                              (
                                                /*context =*/ Main.this,
                                                /*text =*/ getString(R.string.paste_nan),
                                                /*duration =*/ android.widget.Toast.LENGTH_SHORT
                                              ).show();
                                          } /*try*/
                                      } /*if*/
                                  } /*run*/
                              } /*Runnable*/
                        :
                            new Runnable()
                              {
                                public void run()
                                  {
                                    Clipboard.setText
                                      (
                                        Global.FormatNumber
                                          (
                                            Slide.GetScale(LocalWhichScale)
                                                .ValueAt(Slide.GetCursorPos(LocalWhichScale))
                                          )
                                      );
                                  } /*run*/
                              } /*Runnable*/
                      );
                  } /*for*/
                if (Pasting)
                    break;
                Pasting = true;
              } /*for*/
        break;
        case Scales:
            android.widget.Toast.makeText
              (
                /*context =*/ Main.this,
                /*text =*/ R.string.not_set_scale,
                /*duration =*/ android.widget.Toast.LENGTH_SHORT
              ).show();
        break;
          } /*switch*/
      } /*CreateContextMenu*/

    public void OnScaleNameClick
      (
        int /*SCALE.**/ WhichScale
      )
      {
        ScalePicker.Launch
          (
            /*Caller =*/ Main.this,
            /*WhichScale =*/ WhichScale,
            /*RequestCode =*/ SetFirstScaleRequest + WhichScale,
            /*CurScaleName =*/ Slide.GetScale(WhichScale).Name()
          );
      } /*OnScaleNameClick*/

    @Override
    public boolean onOptionsItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = OptionsMenu.get(TheItem);
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onOptionsItemSelected*/

    @Override
    public boolean onContextItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = ContextMenu.get(TheItem);
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onContextItemSelected*/

    @Override
    public void onActivityResult
      (
        int RequestCode,
        int ResultCode,
        android.content.Intent Data
      )
      {
        ScalePicker.Cleanup();
        if (ResultCode != android.app.Activity.RESULT_CANCELED)
          {
            final RequestResponseAction Action = ActivityResultActions.get(RequestCode);
            if (Action != null)
              {
                Action.Run(ResultCode, Data);
              } /*if*/
          } /*if*/
      } /*onActivityResult*/

    @Override
    public void onSaveInstanceState
      (
        android.os.Bundle ToSaveInstanceState
      )
      {
        super.onSaveInstanceState(ToSaveInstanceState);
        ToSaveInstanceState.putAll(Slide.SaveState());
      } /*onPause*/

    @Override
    public void onRestoreInstanceState
      (
        android.os.Bundle SavedInstanceState
      )
      {
        Slide.RestoreState(SavedInstanceState);
        super.onRestoreInstanceState(SavedInstanceState);
      } /*onResume*/

  } /*Main*/
