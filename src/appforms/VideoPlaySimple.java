package appforms;

import android.app.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.LEORChn.SimpleBili.*;
import static com.LEORChn.SimpleBili.R.id.*;
import java.lang.reflect.*;
import java.util.*;
import leorchn.lib.*;

import leorchn.lib.CrashHandlerReg;
import static leorchn.lib.WidgetOverride.*;
import static android.media.MediaPlayer.*;
import static leorchn.lib.Global.*;
import android.provider.Settings;
import java.text.*;
import simplebili.lib.*;

public class VideoPlaySimple extends Activity implements View.OnClickListener,View.OnTouchListener,MessageQueue.IdleHandler,OnErrorListener,OnPreparedListener,OnBufferingUpdateListener,OnCompletionListener,DanmakuViewControl.OnDanmakuLogListener {
	Activity This; public Activity getContext(){return This;}
	String path="",title="",partname="",vid="",cid="",cookie="",
	referer="Referer: http://www.bilibili.com/video/\r\n",
	useragent="USER-AGENT: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36\r\n";
	//This Class Only For Online Video!!!
	int hasinit=0; DanmakuViewControl dvc;
	boolean readyplay=false;int readydanmaku=0;//0=准备加载，1=不开启弹幕，2=弹幕错误，3=弹幕完成
	protected void onCreate(Bundle sis){
		super.onCreate(sis);
		CrashHandlerReg.reg(this);
		This=this;
		Looper.myQueue().addIdleHandler(this);
	}
	public boolean queueIdle(){
		switch(hasinit){
			case 0://初始化界面
				dvc=new DanmakuViewControl(this,R.layout.activity_video_play_simple,R.id.vplay_danmakuview,50);
				setContentView(dvc.getBackRootView());
				vp=(VideoView)fv(vplay_player);
				inf=(TextView)fv(vplay_timeinfo);
				totaltime=(TextView)fv(vplay_totaltime);
				pb=(ProgressBar)fv(vplay_progress);
				panelfin=fv(vplay_panelfinish); visible(panelfin,false);
				preload=fv(vplay_preload);
				playdbg=(TextView)fv(vplay_playerdbg); 
				dandbg=(TextView)fv(vplay_dandbg); dvc.setOnDanmakuLogListener(this);
				scltip=(TextView)fv(vplay_scrolldisplay);
				itv=(ImageView)fv(vplay_animtv);//加载电视动画
				itv.setBackgroundResource(R.drawable.anim_bilitv);//设置为动态
				AnimationDrawable bg=(AnimationDrawable)itv.getBackground();
				bg.start();
				path=getIntent().getStringExtra("path");
				cid=getIntent().getStringExtra("cid");
				//loadStringParam(new String[]{path,title,partname,vid,cid,cookie},"path,title,partname,vid,cid,cookie".split(","));
				//clearStringNull(title,partname,vid,cid,cookie);
				break;
			case 1://加载日志
				dandbg.setText(playdbg.getText(),TextView.BufferType.EDITABLE);
				dvc.downAndLoadDanmaku("http://comment.bilibili.com/"+cid+".xml");
				playdbg.setText(playdbg.getText(),TextView.BufferType.EDITABLE);
				plog("正在准备播放："+path+"\n");
				plog("正在初始化【在线】播放环境...\n");
				break;
			case 2:
				try{
					int sp=Integer.parseInt(Uri.parse(path).getQueryParameter("rate"));
					if(sp>4500)sp=sp/1000;
					tip("被B站限制缓冲速度 "+sp+" kb");
				}catch(Exception e){plog("B站限速信息读取异常。\n");}
				mhead=new HashMap<String,String>();
				if(android.os.Build.VERSION.SDK_INT >20){
					mhead.put("Referer",referer.split(": ")[1]);
					mhead.put("USER-AGENT",useragent.split(": ")[1]);
					setVideoURI(Uri.parse(path),mhead);
				}else{
					mhead.put("USER-AGENT",useragent.split(": ")[1]+referer);
					setVideoURI(Uri.parse(path),mhead);
				}
				//if(false){ entertempdownplay(mhead); hasinit++; return false; }
				plog("在线播放环境初始化完成，正在缓冲...\n");
				break;
			case 3:
				vp.setOnErrorListener(this);
				vp.setOnPreparedListener(this);
				vp.setOnCompletionListener(this);
				//vp.setOnGenericMotionListener(this);
				readyplay=true;
				if(readydanmaku>0)vp.start(); //如果未禁用并且在加载中则等待弹幕加载完成或出错
		}
		hasinit++;
		return hasinit<10;
	}
/*	void entertempdownplay(Map<String,String>head){
		tip("系统版本(高于20)："+android.os.Build.VERSION.SDK_INT);
		playpath=TempDownPlay.start(this,path,head);
		if(playpath.isEmpty())return;
		vp.setVideoPath(playpath);
	}*/
	void retryplay(){ 
		readyplay=false; if(readydanmaku>1)readydanmaku=0;
		hasinit=0; Looper.myQueue().addIdleHandler(this);
	}
	View preload; TextView playdbg,dandbg,scltip;
	VideoView vp; MediaPlayer mp; ProgressBar pb; TextView inf; TextView totaltime; ImageView itv;
	View panelfin;
	//监听器 开始
	Map<String,String>mhead; //boolean reseturl_android5=false;
	public boolean onError(MediaPlayer p1, int what, int extra) { 
		plog("播放器错误，按菜单键重试...what="+what+",extra="+extra);
		return true;
	} final int progbarAccurate=5;//进度条精度
	public void onPrepared(MediaPlayer p1){//开始播放视频！
		visible(preload,false);
		mp=p1;pb.setMax(100*progbarAccurate);
		p1.setScreenOnWhilePlaying(true);
		p1.setOnBufferingUpdateListener(this);
		vp.setBackgroundColor(android.R.color.transparent);
		vp.setOnTouchListener(this);
		totaltime.setText("/"+Formater.format(p1.getDuration()));
		init_Ges();
	}
	public void onBufferingUpdate(MediaPlayer p1, int p2) { 
		buffprog=p2; keepUpdateThreadAlive();//progressupdate();
	}
	public void onCompletion(MediaPlayer p1){
		btnbind(vplay_endplay,vplay_backward15,vplay_replay,vplay_panelhide,vplay_nextpart);
		visible(panelfin,true);
	}
	public void onClick(View v){switch(v.getId()){
		case vplay_endplay: finish(); break;
		case vplay_backward15: seekto(vp.getDuration()-15000); visible(panelfin,false); vp.start(); break;
		case vplay_replay: seekto(0); visible(panelfin,false); vp.start(); break;
		case vplay_panelhide: visible(panelfin,false); break;
		case vplay_nextpart: break; //TODO
	}}
	GestureDetector ges; int scrolltype=0,sclreal=0; double scrollval=0;//0=并无滚动。1=滚动进度条。2=滚动亮度。3=滚动音量
	float volume=1;
	final int progmtp=10,brigmtp=20,volmtp=20;//滑动屏幕的作用倍数
	void init_Ges(){
		ges=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
			@Override public boolean onScroll(MotionEvent et1,MotionEvent et2,float x,float y){
				if(scrolltype==0) //只在第一次滚动时触发判断是何种滚动
					if(Math.abs(x)>Math.abs(y)){// 左右移动，调整进度
						scrollval=0;
						scrolltype=1; vp.pause();
					}else if(et1.getX()<(vp.getWidth()/2)){ //在屏幕左侧移动，调整亮度
						scrollval=brigmtp*getbright()/17;
						scrolltype=2;
					}else{ //屏在幕右侧移动，调整音量
						scrollval=volmtp*volume*15;
						scrolltype=3;
					}
				visible(scltip,true);
				switch(scrolltype){
					case 1:
						scrollval-=x;
						sclreal=(int)Math.floor(scrollval/progmtp);
						//int curpos=...;
						sclfix(progmtp,0-(vp.getCurrentPosition()/1000),(vp.getDuration()-vp.getCurrentPosition())/1000);
						scltip.setText(//已修BUG:可倒退到0秒以下、以及显示文本不居中
							Formater.format(vp.getCurrentPosition()+(sclreal*1000))+totaltime.getText()+"\n"+
							sclreal+(Math.abs(sclreal)>4?" 秒 进度":" 秒 无效"));
						break;
					case 2://亮度
						scrollval+=y;
						sclfix(brigmtp,0,15);
						setbright(sclreal);
						scltip.setText("亮度 "+sclreal);
						break;
					case 3://音量
						scrollval+=y; 
						sclfix(volmtp,0,15);
						volume=sclreal/15f;
						mp.setVolume(volume,volume);//vol.setProgress(sclreal);
						scltip.setText("音量 "+sclreal);
				}
				return false;
			}
				public boolean onDoubleTap(MotionEvent et) {if(vp.isPlaying())vp.pause();else vp.start();visible(panelfin,false);return false;}
			//public boolean onDoubleTapEvent(MotionEvent p1){return false;}//双击后但并未结束时的其他手势，绑定于双击事件
			//public boolean onSingleTapConfirmed(MotionEvent et) {return false;}
		});
	}
	public boolean onTouch(View p1, MotionEvent et) {
		if(et.getAction()==et.ACTION_UP){
			if(scrolltype==1){
				if(Math.abs(sclreal)>4)//拖进度条的无效范围
					seekto(vp.getCurrentPosition()+sclreal*1000);//隐患：跳转到范围外的时间、播放完毕后重新跳转不隐藏面板
				visible(panelfin,false);
				vp.start();
			}
			visible(scltip,false);
			scrolltype=0;
			////tipoff();
		}
		ges.onTouchEvent(et); return true;
	}
	//int launchtms=0;
	public void onDanmakuLog(String log) { dlog(log); }
	public void onDanmakuLoadCompleted() {
		readydanmaku=3;//弹幕完成
		if(readyplay)vp.start();
	}
	public void onDanmakuLoadError(int errCode,String errmsg){
		readydanmaku=2;//弹幕错误
		if(readyplay)vp.start();
	}
	
	//监听器 结束
	void setbright(int b){
		Settings.System.putInt(this.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,b*17);
	}
	int getbright(){
		return Settings.System.getInt(this.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,18);
	}
	
	void sclfix(int multiple,int realmin,int realmax){ 
		if(scrollval<realmin*multiple)scrollval=realmin*multiple;
		if(scrollval>realmax*multiple)scrollval=realmax*multiple;
		sclreal=(int)Math.floor(scrollval/multiple);
	}
	static class Formater{
		static String format(long i){int sec=(int)Math.floor((i % 60000)/1000);return ((int)Math.floor(i / 60000))+":"+(sec<10?"0"+sec:sec);}
	}
	int buffprog=0;
	void progressupdate(){
		pb.setProgress((vp.getCurrentPosition()*100*progbarAccurate)/vp.getDuration());
		pb.setSecondaryProgress(buffprog*progbarAccurate);
		inf.setText(Formater.format(vp.getCurrentPosition()));
		if(readydanmaku==3)dvc.updateDanmaku(vp.getCurrentPosition()); //弹幕完成时才能updated
	}
	Thread updatethread=new Thread(){public void run(){
		Runnable updaterun=new Runnable(){public void run(){progressupdate();}};
		while(true){
			This.runOnUiThread(updaterun);
			try{Thread.sleep(1000);}catch(Exception e){}
		}
	}};
	void keepUpdateThreadAlive(){ if(!updatethread.isAlive()) updatethread.start(); }
	void setVideoURI(Uri uri,Map<String,String>mHeaders){
		try{
			Class vpclass=Class.forName("android.widget.VideoView");
			Method[]ms=vpclass.getMethods();
			Method mTarget=null;
			for(Method m:ms){
				if(m.getName().equals("setVideoURI"))
					if(m.getParameterTypes().length==2) mTarget=m;
			}
			if(mTarget==null)
				信息框(this,"Set Headers Fail","很抱歉！\n您的系统不支持覆盖此方法，视频可能无法正常播放。","ok");
			else
				mTarget.invoke(vp,new Object[]{uri,mHeaders});
		}catch(Exception e){信息框(this,"Set Headers Fail",Arrays.toString(e.getStackTrace()),"ok");}
	}
	long lastReqExit=0;
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
			case 4://返回键
				if (System.currentTimeMillis() - lastReqExit < 3000) {
					TempDownPlay.end();
					finish();
				} else {
					tip("再按一次退出播放。");
					lastReqExit = System.currentTimeMillis();
				}break;
		}
		return false;//super.onKeyDown(keyCode, event);
	}
	final int menuidx=Menu.FIRST;
	public boolean onCreateOptionsMenu(Menu menu) {
		String[]menus="重试,启动附加功能...".split(",");
		int[]menuicons={R.drawable.topmenu_refresh, R.drawable.topmenu_extmodule};
		enableMenuIcon(menu);
		for(int i=0;i<menus.length;i++)
			menu.add(0,menuidx+i,0,menus[i]).setIcon(menuicons[i]);
		return super.onCreateOptionsMenu(menu);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
		this.closeOptionsMenu();
		switch(item.getItemId()-menuidx){
			case 0: retryplay(); break;
			case 1: 附加功能(this); break;
			default: break;
		}return super.onOptionsItemSelected(item);
	}
	void seekto(int t){ t=t<1000?0:t; vp.seekTo(t); dvc.onVideoSeekto(t); }
	
	void btnbind(int...id){for(int btnid:id)btnbind(btnid);}
	void btnbind(int id){fv(id).setOnClickListener(this);}
	View fv(int id){return findViewById(id);}
	void plog(String s){playdbg.append(s);}
	void dlog(String s){dandbg.append(s);}
	void clearStringNull(String...s){for(String s2:s)if(s2==null)s2="";}
	void loadStringParam(String[]s,String[]id){for(int i=0,sl=s.length;i<sl;i++)s[i]=getIntent().getStringExtra(id[i]);}
}
