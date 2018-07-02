package hyj.autooperation;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiAutomatorTestCase;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hyj.autooperation.common.FilePathCommon;
import hyj.autooperation.conf.WindowOperationConf;
import hyj.autooperation.model.NodeInfo;
import hyj.autooperation.model.WindowNodeInfo;
import hyj.autooperation.model.Wx008Data;
import hyj.autooperation.thread.TemplateThread;
import hyj.autooperation.util.AutoUtil;
import hyj.autooperation.util.DragImageUtil2;
import hyj.autooperation.util.FileUtil;
import hyj.autooperation.util.LogUtil;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 * bug 获取某些节点可能出现 android.support.test.uiautomator.StaleObjectException
 * 如：uiObject2!=null ;
 *    uiObject2.getText();
 *
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private UiDevice mDevice;
    private Context appContext;
    Instrumentation instrumentation;

    @Before
    public void init(){
        appContext = InstrumentationRegistry.getTargetContext();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        mDevice = UiDevice.getInstance(instrumentation);
    }

    public void initAuto(String tag){
        killAndClearWxData();
        startAriPlaneMode(1000);
        currentWx008Data = tellSetEnvirlmentAndGet008Data(tag);
        startWx();
    }

    Wx008Data currentWx008Data;
    String windowText;
    String otherOperationName="";//当前额外动作
    List<WindowNodeInfo> otherOperations = null;
    int actionNo=0;//记录点击所处在位置
    @Test
    public void useAppContext(){
        mDevice.pressHome();
        initAuto("retry");
        //Map<String,WindowNodeInfo> ops = WindowOperationConf.getOperatioByAutoType("养号");
        Map<String,WindowNodeInfo> ops = WindowOperationConf.getOperatioByAutoType("注册");
        List<String> otherOperationNames = new ArrayList<String>();
        otherOperationNames.add("发圈");
        while (true){
            AutoUtil.sleep(1000);
            System.out.println("running-->otherOperation："+otherOperationName+" actionNo:"+actionNo);
            windowText = getAllWindowText("com.tencent.mm");
            //String windowText =getAllWindowText1();
            System.out.println("running-->getAllWindowText："+windowText);
            if(windowText.contains("正在登录...")||windowText.contains("正在载入数据...")) continue;

            if("".equals(otherOperationName)){
                WindowNodeInfo wni = getWniByWindowText(ops,windowText);
                if(wni==null){
                    System.out.println("doAction-->windowText没有匹配ops动作");
                    continue;
                }
                System.out.println("running-->wni："+JSON.toJSONString(wni));
                doAction(wni);
                if("自定义-登录异常".equals(wni.getOperation())&&wni.isWindowOperatonSucc()){
                    initAuto("next");
                }else if("自定义-判断登录成功".equals(wni.getOperation())&&wni.isWindowOperatonSucc()){
                    if(otherOperationNames.size()==0){
                        int i = 0;
                        while (i<10){
                            AutoUtil.sleep(1000);
                            System.out.println("doAction-->登录成功等待秒数 "+i);
                            ++i;
                        }
                        initAuto("next");//没有其他动作，下一个
                    }else {
                        otherOperationName = otherOperationNames.get(0);
                        otherOperations = WindowOperationConf.getOtherOperationByAutoType(otherOperationName);
                    }
                }
                continue;
            }else {//成功执行其他动作
                if(actionNo>otherOperations.size()-1){
                    actionNo=0;
                    if(otherOperationNames.indexOf(otherOperationName)+1<=otherOperationNames.size()-1){
                        otherOperationName = otherOperationNames.get(otherOperationNames.indexOf(otherOperationName)+1);
                        otherOperations = WindowOperationConf.getOtherOperationByAutoType(otherOperationName);
                    }else {//执行完所有额外动作
                        otherOperationName="";
                        initAuto("next");//没有其他动作，下一个
                        continue;
                    }
                }
                WindowNodeInfo otherWni = otherOperations.get(actionNo);
                doAction(otherWni);
                if(otherWni.isWindowOperatonSucc()){
                    actionNo = actionNo +1;
                }
            }
        }
    }
    @Test
    public void test1(){
        while (true){
            windowText = getAllWindowText("com.tencent.mm");
            //String windowText =getAllWindowText1();
            System.out.println("doA-- running-->getAllWindowText："+windowText);

            WindowNodeInfo windowNodeInfo3 = new WindowNodeInfo("6.6.7","发圈","这一刻的想法...","长按拍照分享");
            NodeInfo nodeInfo31 = new NodeInfo(6,"","拍照分享","长按拍照分享");
            windowNodeInfo3.getNodeInfoList().add(nodeInfo31);

            break;

          /* UiObject uiObject = findNodeByText("发现");
            try {
                uiObject.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }*/
        }
    }

    //自定义、通用两种
    public void doAction(WindowNodeInfo wni){
        if(wni.getOperation().contains("自定义-")){
            doCustomerAction(wni);
        }else {
            for(NodeInfo nodeInfo: wni.getNodeInfoList()){
                switch (nodeInfo.getNodeType()){
                    case 1:
                        nodeInfo.setOperationSucc(doClick(nodeInfo));
                        AutoUtil.sleep(nodeInfo.getNodeOperationSleepMs());
                        break;
                    case 6:
                        nodeInfo.setOperationSucc(doLongClick(nodeInfo));
                        break;
                }
            }
            wni.setWindowOperatonSucc(validIsAllTrue(wni));//设置总成功标志
        }
        System.out.println("doAction-->"+wni.toString());
    }
    public boolean validIsAllTrue(WindowNodeInfo wni){
        if(wni.getNodeInfoList().size()==0) return false;
        for(NodeInfo nodeInfo:wni.getNodeInfoList()){
            if(!nodeInfo.isOperationSucc()){
                return false;
            }
        }
        return true;
    }
    public void doCustomerAction(WindowNodeInfo wni){
        String operationDesc="";
        boolean isOperationsSucc = false;
        if("自定义-点击注册2".equals(wni.getOperation())){
            List<UiObject2> uos = findNodesByClaZZ(EditText.class);
            if(uos!=null&&uos.size()==3){
                uos.get(0).setText("1123");
                uos.get(1).setText("136521598"+new Random().nextInt(10)+new Random().nextInt(10));
                uos.get(2).setText("789lkjmnhikj");//密码
                isOperationsSucc = clickUiObjectByText("注册");
            }
            operationDesc = "输入账号，输入密码，点击【注册】"+isOperationsSucc;
        }else if("自定义-过滑块".equals(wni.getOperation())){
            int dragEndX=0;
            cmdScrrenShot();//截图
            Bitmap bi = waitAndGetBitmap();
            dragEndX = DragImageUtil2.getPic2LocX(bi);
            operationDesc="拖动dragEndX"+dragEndX;
            Point[] points = getDargPoins(235,dragEndX+63,1029);
            boolean dragFlag = mDevice.swipe(points,100);
            AutoUtil.sleep(3000);
        }else if("自定义-输入账号密码".equals(wni.getOperation())){
            if(validEnviroment()){
                System.out.println("doAction--->改机成功");
                List<UiObject2> uos = findNodesByClaZZ(EditText.class);
                if(uos!=null&uos.size()==2){
                    System.out.println("runn size："+uos.size());
                    String wxid = currentWx008Data.getPhone();
                    String pwd = currentWx008Data.getWxPwd();
                    if(!TextUtils.isEmpty(currentWx008Data.getWxId())){
                        wxid = currentWx008Data.getWxId();
                    }else if(!TextUtils.isEmpty(currentWx008Data.getWxid19())){
                        wxid = currentWx008Data.getWxid19();
                    }
                    uos.get(0).setText(wxid);
                    uos.get(1).setText(TextUtils.isEmpty(pwd)?"nullnull":pwd);
                    isOperationsSucc = clickUiObjectByText("登录");
                    operationDesc = "输入账号["+wxid+"]密码["+pwd+"]点击登录"+isOperationsSucc;
                    AutoUtil.sleep(5000);
                }
            }else {
                operationDesc = "改机失败";
            }
        }else if("自定义-登录下一步".equals(wni.getOperation())){
            UiObject2 uiObject2 = mDevice.findObject(By.text("下一步"));
            int y = uiObject2.getVisibleBounds().top-uiObject2.getVisibleBounds().height();
            int x = uiObject2.getVisibleBounds().width()/2;
            mDevice.click(x,y);
            operationDesc = "点击用微信号/QQ号/邮箱登录 x:"+x+" y:"+y;
        }else if("自定义-登录异常".equals(wni.getOperation())){
            operationDesc = wni.getMathWindowText();
            isOperationsSucc = true;
        }else if("自定义-判断登录成功".equals(wni.getOperation())){
            operationDesc = "登录成功";
            isOperationsSucc = true;
        }else if("自定义-点我知道了".equals(wni.getOperation())){
            UiObject2 uiObject2 = mDevice.findObject(By.textContains(wni.getMathWindowText()));
            int x = uiObject2.getVisibleBounds().centerX();
            int y = uiObject2.getVisibleBounds().bottom+(uiObject2.getVisibleBounds().height()*2);
            mDevice.click(x,y);
            operationDesc = "点我知道了 x:"+x+" y:"+y;
            isOperationsSucc = true;
        }else if("自定义-输入发圈内容".equals(wni.getOperation())){
            String inputText = "558 "+currentWx008Data.getPhone();
            UiObject2 uiObject2 = mDevice.findObject(By.textContains(wni.getMathWindowText()));
            uiObject2.setText(inputText);
            mDevice.findObject(By.text("发表")).click();
            operationDesc = "输入发圈内容："+inputText;
            isOperationsSucc = true;
            AutoUtil.sleep(5000);
        }
        wni.setWindowOperationDesc(operationDesc);
        wni.setWindowOperatonSucc(isOperationsSucc);
    }

    //判断改机是否成功 改机成功返回true
    private boolean validEnviroment(){
        String phoneTag = FileUtil.readAllUtf8(FilePathCommon.phoneTagPath);
        String phoneTag008 = TextUtils.isEmpty(currentWx008Data.getPhone())?currentWx008Data.getWxId():currentWx008Data.getPhone();
        System.out.println("phoneTag-->"+phoneTag+" phoneTag008:"+phoneTag008);
        if(!phoneTag.equals(phoneTag008)){
            LogUtil.login(" exception change phone fail",currentWx008Data.getPhone()+" "+currentWx008Data.getWxId()+" "+currentWx008Data.getWxPwd());
            return false;
        }else {
            return true;
        }
    }

    public boolean doInputText(NodeInfo nodeInfo){
        if(!TextUtils.isEmpty(nodeInfo.getNodeText())){
             return setUiObjectTextByText(nodeInfo.getNodeText(),"556");
        }
        return false;
    }

    public boolean doLongClick(NodeInfo nodeInfo){
        if(!TextUtils.isEmpty(nodeInfo.getNodeDesc())){
            return longClickUiObjectByDesc(nodeInfo.getNodeDesc());
        }else if(!TextUtils.isEmpty(nodeInfo.getNodeText())){
            return longClickUiObjectByText(nodeInfo.getNodeText());
        }
        return false;
    }

    public boolean doClick(NodeInfo nodeInfo){
        if(!TextUtils.isEmpty(nodeInfo.getNodeText())){
            if(nodeInfo.getNodeText().contains("%")){
                //clickUiObjectByText(nodeInfo.getNodeText());
            }
            return clickUiObjectByText(nodeInfo.getNodeText());
        }else if(!TextUtils.isEmpty(nodeInfo.getNodeDesc())){
            if(nodeInfo.getNodeDesc().contains("%")){
                return clickNodeByDesContain(nodeInfo.getNodeDesc().replaceAll("%",""));
            }
            return clickUiObjectByDesc(nodeInfo.getNodeDesc());
        }
        return false;
    }

    public WindowNodeInfo getWniByWindowText(Map<String,WindowNodeInfo>  ops,String windowText){
        for(String key:ops.keySet()){
            if(key.contains("|")){
                String[] arr = key.split("\\|");
                boolean flag = true;
                for(String str :arr){
                    if(!windowText.contains(str)){
                        flag = false;
                        break;
                    }
                }
                if(flag) return ops.get(key);
                continue;
            }else if(windowText.contains(key)){
                return ops.get(key);
            }
        }
        return null;
    }

    @Test
    public void testGetContent() throws Exception {
        while (true){
            getAllWindowText("com.tencent.mm");
        }
    }
    //获取当前窗口的所有文本，耗时200毫秒
    public String getAllWindowText(String pkName){
        System.out.println("debug--->start============getAllWindowText================");
        String windowContent = "";
        try {
            List<UiObject2> cbObjs = mDevice.findObjects(By.pkg(pkName));
            if(!cbObjs.isEmpty()){
                for(UiObject2 obj:cbObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
        System.out.println("debug--->end============getAllWindowText================\n");
        }catch (Exception e){
            System.out.println("debug--->end StaleObjectException============getAllWindowText================\n");
            e.printStackTrace();
        }
        return windowContent;
    }

    public String newGetAllTextByClass(){
        String allText = getAllTextByClass(Button.class)+getAllTextByClass(EditText.class)+getAllTextByClass(View.class);
        return allText;
    }

    public String getAllTextByClass(Class clazz){
        String windowContent = "";
        try {
            List<UiObject2> cbObjs = mDevice.findObjects(By.clazz(clazz));
            if (!cbObjs.isEmpty()) {
                for (UiObject2 obj : cbObjs) {
                    windowContent = windowContent + getNotNullComponentText(obj);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return windowContent;
    }
    public String getAllWindowText1(){
        System.out.println("debug--->start============getAllWindowText1================");
        String windowContent = "";
        try {

            List<UiObject2> cbObjs = mDevice.findObjects(By.clazz(CheckBox.class));
            if(!cbObjs.isEmpty()){
                for(UiObject2 obj:cbObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
            List<UiObject2> edtObjs = mDevice.findObjects(By.clazz(EditText.class));
            List<UiObject2> trObjs = mDevice.findObjects(By.clazz(TextView.class));
            List<UiObject2> viewObjs = mDevice.findObjects(By.clazz(View.class));
            List<UiObject2> btnObjs = mDevice.findObjects(By.clazz(Button.class));
            if(!btnObjs.isEmpty()){
                for(UiObject2 obj:btnObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
            if(!viewObjs.isEmpty()){
                for(UiObject2 obj:viewObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
            if(!edtObjs.isEmpty()){
                for(UiObject2 obj:edtObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
            if(!trObjs.isEmpty()){
                for(UiObject2 obj:trObjs){
                    windowContent = windowContent +getNotNullComponentText(obj);
                }
            }
            System.out.println("debug--->end============getAllWindowText1================\n");
        }catch (Exception e){
            System.out.println("debug--->end StaleObjectException============getAllWindowText1================\n");
            e.printStackTrace();
        }
        return windowContent;
    }
    //获取非空文本
    public String getNotNullComponentText(UiObject2 obj){
        String result = "";
        try {
            //System.out.println(" text:"+obj.getText()+" desc:"+obj.getContentDescription());
            System.out.println("debug--->"+obj.getClassName()+" text:"+obj.getText()+" desc:"+obj.getContentDescription()+" pgName:"+obj.getApplicationPackage()+" resName:"+obj.getResourceName()+" childCount:"+obj.getChildCount()+" isclick:"+obj.isClickable());
            if(!TextUtils.isEmpty(obj.getText())) result = result+obj.getText()+"|";
            if(!TextUtils.isEmpty(obj.getContentDescription())) result = result+obj.getContentDescription()+"|";
        }catch (Exception e){
            System.out.println("debug--->end  getText StaleObjectException==============================\n");
            e.printStackTrace();
        }
        return result;
    }
    @Test
    public void useAppContext1() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        System.out.println("getPackageName-->"+appContext.getPackageName());
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        while (true){
            Thread.sleep(1000);
            System.out.println("test runing-->"+System.currentTimeMillis());

            UiSelector uiselector2 = new UiSelector().text("注册");
            UiObject uiobject2 = new UiObject(uiselector2);
            if(uiobject2.exists()){
                try {
                    boolean flag = uiobject2.click();
                    AutoUtil.sleep(1000);
                    boolean flagBack = mDevice.pressBack();
                    System.out.println("test runing-->clickFlag:"+flag+" backFlag:"+flagBack);
                    String  path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Screenshots";
                    FileUtil.writeContent2FileForceUtf8(FilePathCommon.fkFilePath,"1");
                    File file = waitAndGetFile(path);
                    if(file!=null){
                        Bitmap bi = BitmapFactory.decodeFile(path+"/"+file.getName());
                        System.out.println("fileSize runing-->"+file.length()+" fileName:"+file.getName()+" width:"+bi.getWidth()+" height:"+bi.getHeight());
                        System.out.println("delete runing-->"+file.delete());

                        File file0 = waitAndGetFile(path);
                        if(file0!=null){
                            System.out.println("fileSize0 runing-->"+file0.length()+" fileName:"+file0.getName());
                        }else {
                            System.out.println("fileSize0 runing-->null");
                        }
                    }
                } catch (UiObjectNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    private File waitAndGetFile(String path){
        File picFile = null;
        File[] files = new File(path).listFiles();
        if(files!=null&&files.length>0){
            picFile = files[files.length-1];
        }
        return picFile;
    }
    private File delAllFiles(String path){
        File picFile = null;
        File[] files = new File(path).listFiles();
        for(File f:files){
            f.delete();
        }
        return picFile;
    }

    public void testScreenShot(UiDevice mUIDevice) {
        //UiDevice mUIDevice =  UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if(mUIDevice!=null){
            while (true){
                System.out.println("版本1 开始截图1 imgName-->");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                File file3s = new File("/sdcard/azy/aa.txt");
                try {
                    file3s.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String imgName = "16666new1"+System.currentTimeMillis()+".png";
                File files = new File("/sdcard/azy/"+imgName);
                mUIDevice.takeScreenshot(files);
                System.out.println("截图成功2 imgName-->"+imgName);
            }
        }
    }

    String action="";
    @Test
    public void useAppContext2() {

        killAndClearWxData();
        startWx();

        while (true){
            System.out.println("running...."+mDevice.getCurrentPackageName()+" traver:"+mDevice.getLastTraversedText()+" pdName:"+mDevice.getProductName()+" action:"+action);
            boolean flag1 = clickUiObjectByText("注册");
            System.out.println("running点击注册1："+flag1);
            List<UiObject2> uos = findNodesByClaZZ(EditText.class);
            if(uos!=null&&uos.size()==3){
                uos.get(0).setText("1123");
                uos.get(1).setText("136521598"+new Random().nextInt(10)+new Random().nextInt(10));
                uos.get(2).setText("789lkjmnhikj");//密码
                boolean clickFlag2 = clickUiObjectByText("注册");
                System.out.println("running点击注册2："+clickFlag2);
            }
            boolean clickFlag3 = clickNodeByDesContain("我已阅读并同意上述条款");
            boolean clickFlag4 = clickUiObjectByDesc("下一步");
            System.out.println("running点击同意条款下一步："+clickFlag4);
            boolean clickFlag5 = clickUiObjectByDesc("开始");
            System.out.println("running点击开始安全验证："+clickFlag5);

            UiObject2  tdText = mDevice.findObject(By.desc("拖动下方滑块完成拼图"));
            if(tdText!=null){
                int dragEndX=0;
                while (dragEndX==0){
                    System.out.println("running-->tdText:"+tdText.getContentDescription());
                    cmdScrrenShot();//截图
                    Bitmap bi = waitAndGetBitmap();
                    dragEndX = DragImageUtil2.getPic2LocX(bi);
                    System.out.println("running-->dragEndX:"+dragEndX);
                    Point[] points = getDargPoins(235,dragEndX+63,1029);
                    mDevice.swipe(points,100);
                }
            }
            UiObject2 qrWindow = mDevice.findObject(By.descContains("联系符合以下条件的"));
            if(qrWindow!=null){
                killAndClearWxData();
                startWx();
            }
        }
    }

    public Bitmap waitAndGetBitmap(){
        String  path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Screenshots";
        File file = waitAndGetFile(path);
        Bitmap bi = null;
        while (file==null||bi==null){
            AutoUtil.sleep(1000);
            file = waitAndGetFile(path);
            if(file!=null){
                String pngPath = path+"/"+file.getName();
                System.out.println("running-->pngPath:"+pngPath);
                bi = BitmapFactory.decodeFile(pngPath);
            }
            System.out.println("running-->等待图片生成:");
        }
        return bi;
    }

    public Point[] getDargPoins(int startX,int endY,int locY){
        Point point0 = new Point();
        Point point1 = new Point();
        Point point2 = new Point();
        point0.set(startX,locY);
        point1.set(endY-70,locY);
        point2.set(endY,locY);
        Point[] points = {point0,point1,point2};
        return points;
    }

    public void startAriPlaneMode(long sleepMs){
        System.out.println("doAction-->开启飞行模式");
        exeShell("settings put global airplane_mode_on 1" );
        exeShell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true" );
        AutoUtil.sleep(sleepMs);
        exeShell("settings put global airplane_mode_on 0");
        exeShell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false");
        AutoUtil.sleep(5000);
        boolean flag = isConnectInternet();
        while (!flag){
            AutoUtil.sleep(800);
            flag = isConnectInternet();
            System.out.println("doAction-->开启飞行模式-等待网络恢复");
        }
    }
    public void startWx(){
        startAppByPackName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
        AutoUtil.sleep(800);
    }

    public  void startAppByPackName(String packageName,String activity){
        Intent intent = new Intent();
        ComponentName cmp=new ComponentName(packageName,activity);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        System.out.println("running-->start app:"+packageName+" activity:"+activity);
        appContext.startActivity(intent);
    }
    public Wx008Data tellSetEnvirlmentAndGet008Data(String tag){
        FileUtil.writeContent2FileForceUtf8(FilePathCommon.setEnviromentFilePath,tag);//next登录下一个，retry新登录,首次开启也是retry
        AutoUtil.sleep(800);//等待对方处理写入文件
        String str = FileUtil.readAllUtf8(FilePathCommon.setEnviromentFilePath);
        System.out.println("doAction-->str0:"+str);
        while (str.equals("next")||str.equals("retry")){//等待对方写入hook和008data，对方修改状态，循环不执行
            str = FileUtil.readAllUtf8(FilePathCommon.setEnviromentFilePath);
            System.out.println("doAction-->str1:"+str);
            AutoUtil.sleep(500);
        }
        String wx008DataSstr = FileUtil.readAllUtf8(FilePathCommon.wx008DataFilePath);
        Wx008Data currentWx008Data = JSON.parseObject(wx008DataSstr,Wx008Data.class);
        System.out.println("doAction-->currentWx008Data:"+JSON.toJSONString(currentWx008Data));
        return currentWx008Data;
    }

    public void killAndClearWxData(){
        System.out.println("doAction-->关闭、清楚数据");
        List<String> cmds = getKillAndClearWxCmds();
        for(String cmd:cmds){
            exeShell(cmd);
        }
    }
    public void cmdScrrenShot(){
        exeShell("input keyevent 120");
        AutoUtil.sleep(1000);
    }
    public String exeShell(String cmd){
        try {
            String result =  mDevice.executeShellCommand(cmd);
            System.out.println("running-->cmd:"+cmd+"  ret:"+result);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<String> getKillAndClearWxCmds(){
        List<String> cmds = new ArrayList<String>();
        cmds.add("am force-stop com.tencent.mm" );
        cmds.add("pm clear com.tencent.mm" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/MicroMsg" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_cache" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_dex" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_font" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_lib" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_recover_lib" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/app_tbs" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/cache" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/databases" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/face_detect" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/files" );
        cmds.add("rm -r -f /data/data/com.tencent.mm/shared_prefs" );
        cmds.add("rm -r -f /sdcard/tencent" );
        return cmds;
    }

    public UiObject2 findNodeByBySelector(BySelector bs){
        if(bs!=null){
            UiObject2 uo = mDevice.findObject(bs);
            if(uo!=null) return uo;
        }
        return null;
    }
    public void setUiObject2Text(UiObject2 uo2,String text){
        if(uo2!=null){
            uo2.setText(text);
        }
    }
    public UiObject findNodeByUiSelector(UiSelector us){
        if(us!=null){
            UiObject uo = mDevice.findObject(us);
            if(uo!=null&&uo.exists()) return uo;
        }
        return null;
    }
    public UiObject findNodeByText(String text){
        UiSelector us = new UiSelector().text(text);
        return findNodeByUiSelector(us);
    }
    public UiObject findNodeByDesc(String desc){
        UiSelector us = new UiSelector().description(desc);
        return findNodeByUiSelector(us);
    }
    public UiObject findNodeById(String id){
        UiSelector us = new UiSelector().resourceId(id);
        return findNodeByUiSelector(us);
    }

    public boolean clickUiObject(UiObject uo){
        try {
            if(uo!=null){
                if(uo.isClickable()){
                    return uo.clickAndWaitForNewWindow();
                }else {
                    System.out.println("doAction--->x:"+uo.getBounds().centerX()+" y:"+uo.getBounds().centerY());
                    return mDevice.click(uo.getBounds().centerX(),uo.getBounds().centerY());
                }
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean longClickUiObject(UiObject uo){
        try {
            if(uo!=null){
                if(uo.isLongClickable()){
                    return uo.longClick();
                }
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean setUiObjectText(UiObject uo,String text){
        if(uo!=null&&uo.exists()){
            try {
                return uo.setText(text);
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public boolean clickUiObjectByText(String text){
        return clickUiObject(findNodeByText(text));
    }
    public boolean clickUiObjectByDesc(String desc){
        return clickUiObject(findNodeByDesc(desc));
    }
    public boolean longClickUiObjectByDesc(String desc){
        return longClickUiObject(findNodeByDesc(desc));
    }
    public boolean longClickUiObjectByText(String text){
        return longClickUiObject(findNodeByText(text));
    }
    public boolean setUiObjectTextByDesc(String desc,String text){
        return setUiObjectText(findNodeByDesc(desc),text);
    }
    public boolean setUiObjectTextByText(String uiText,String text){
        return setUiObjectText(findNodeByText(uiText),text);
    }
    public boolean setUiObjectTextById(String id,String text){
        return setUiObjectText(findNodeById(id),text);
    }
    public List<UiObject2> findNodesByClaZZ(Class clazz){
        BySelector bs = By.clazz(clazz);
        if(bs!=null){
            return mDevice.findObjects(bs);
        }
        return null;
    }

    public UiSelector findUiSelectorByDesContain(String desc){
          return new UiSelector().descriptionContains(desc);
    }
    public UiObject findUiObjectByDesContain(String desc){
         return mDevice.findObject(findUiSelectorByDesContain(desc));
    }
    public boolean clickNodeByDesContain(String desc){
        UiObject obj = findUiObjectByDesContain(desc);
        try {
            if(obj!=null&&obj.isClickable()){
                return obj.click();
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    //判断是否联网
    public boolean isConnectInternet(){
        ConnectivityManager connectivityManager = (ConnectivityManager)appContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo == null || !networkInfo.isAvailable()) {
            //当前无可用网络
            return false;
        }else {
            //当前有可用网络
            return true;
        }
    }

    @Test
    public void testVpdpn()  {
        doVpn();
    }
    public void doVpn(){
        String lastAction="init";
        while (true){
            System.out.println("doAction----------------------------------------------------->action:"+lastAction);
            UiObject t1 = mDevice.findObject(new UiSelector().text("失败"));
            if(t1==null){
                System.out.println("doAction--t1 is null");
            }else {
                System.out.println("doActiont1--exeit:"+t1.exists());
                System.out.println("doAction--t1 is not null");
            }
            String pkg = mDevice.getCurrentPackageName();
            String allText = getAllWindowText(pkg);
            getAllWindowText1();
            if(!"com.android.settings".equals(pkg)&&!"com.android.vpndialogs".equals(pkg)){
                System.out.println("doAction-->打开vpn:"+pkg);
                opentActivity(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                continue;
            }

            UiObject2 vpnObj1 =  mDevice.findObject(By.text("VPN"));
            if(vpnObj1!=null&&!allText.contains("添加VPN")){
                mDevice.click(vpnObj1.getVisibleCenter().x,vpnObj1.getVisibleCenter().y);
                lastAction="点击VPN";
                System.out.println("doAction-->点击VPN");
                continue;
            }

            UiObject2 disConnectObj = mDevice.findObject(By.text("断开连接"));
            if(disConnectObj!=null){
                System.out.println("doAction-->点击断开连接");
                lastAction="点击断开连接";
                disConnectObj.click();
                continue;
            }


            if(allText.contains("添加VPN")){
                UiObject2 vpnObj =  mDevice.findObject(By.text("PPTP VPN"));
                if(vpnObj!=null){
                    int  x = vpnObj.getVisibleCenter().x;
                    int  y = vpnObj.getVisibleCenter().y;
                    System.out.println("vpnObj11-->x:"+x+" y:"+y);
                    System.out.println("doAction-->点击PPTP VPN连接");
                    mDevice.click(x,y);
                    lastAction="点击PPTP VPN连接";
                    continue;
                }else if(mDevice.findObject(By.text("已连接"))!=null){
                    if("点击PPTP VPN连接".equals(lastAction)){
                        System.out.println("doAction-->连接成功，退出");
                        break;
                    }else if("init".equals(lastAction)||"点击断开连接".equals(lastAction)||"点击VPN".equals(lastAction)){
                        System.out.println("doAction-->点击[已连接]，弹出窗口");
                        mDevice.click(564,768);
                        continue;
                    }
                }
            }
        }
    }

    public void opentActivity(String acvityName){
        Intent intent = new Intent(acvityName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }


}
