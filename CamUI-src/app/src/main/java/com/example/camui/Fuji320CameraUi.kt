package com.example.camui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import java.util.Locale

internal data class Fuji320CameraUiState(
 val shutter:String,val iso:String,val kelvin:String,val ss01:Float,val iso01:Float,val kelvin01:Float,
 val ev:Float,val evMin:Float,val evMax:Float,val focus:Float,val focusText:String,val zoom:Float,val zoomText:String,
 val battery:Int,val temp:Int,val batteryStatus:String,val filter:String,val filterOpen:Boolean,
 val filters:List<Pair<String,String>>,val luna:String?,val lunaInfo:List<String>,val light:Boolean,val flash:Boolean,
 val ae:Boolean,val af:Boolean,val busy:Boolean,val afX:Float,val afY:Float,val afState:Int,val hideHud:Boolean
)
internal data class Fuji320CameraUiActions(
 val onHud:()->Unit,val onTheme:()->Unit,val onFlash:()->Unit,val onAe:()->Unit,val onAf:()->Unit,
 val onFocus:(Float)->Unit,val onEv:(Float)->Unit,val onSs:(Float)->Unit,val onIso:(Float)->Unit,val onKelvin:(Float)->Unit,
 val onZoom:(Float)->Unit,val onZoomEnd:()->Unit,val onZoomStep:(Int)->Unit,val onAlbum:()->Unit,val onFilter:()->Unit,
 val onFilterSelect:(String)->Unit,val onSettings:()->Unit,val onLuna:()->Unit,val onShutter:()->Unit,
 val onBurstStart:()->Unit,val onBurstEnd:()->Unit
)
private const val F320W=1846f
private const val F320H=852f
private val F320Orange=Color(0xFFF0A030)

@Composable private fun BoxWithConstraintsScope.r320(x:Float,y:Float,w:Float,h:Float):Modifier=
 Modifier.offset(maxWidth*(x/F320W),maxHeight*(y/F320H)).size(maxWidth*(w/F320W),maxHeight*(h/F320H))

@Composable private fun F320Text(text:String,modifier:Modifier=Modifier,color:Color=Color.White,size:TextUnit=14.sp,
 align:TextAlign=TextAlign.Start,bold:Boolean=true,hud:Boolean=false,maxLines:Int=1){
 Text(text,modifier,color=color,fontSize=size,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal,
  fontFamily=if(hud)FontFamily.SansSerif else MaterialTheme.typography.bodyMedium.fontFamily,
  textAlign=align,maxLines=maxLines,overflow=TextOverflow.Ellipsis)
}

@Composable private fun F320Button(modifier:Modifier,label:String,selected:Boolean=false,onClick:()->Unit,icon:@Composable BoxScope.()->Unit){
 Column(modifier.clip(RoundedCornerShape(15.dp))
  .background(Brush.verticalGradient(listOf(Color(0xFF55575A),Color(0xFF333537),Color(0xFF202224))))
  .border(1.dp,if(selected)F320Orange else Color(0xFF777A7D),RoundedCornerShape(15.dp))
  .clickable(onClick=onClick).padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center,content=icon)
  F320Text(label,color=if(selected)F320Orange else Color.White,size=11.sp,align=TextAlign.Center,maxLines=2)
 }
}
@Composable private fun F320Slider(modifier:Modifier,label:String,value:String,fraction:Float,right:String,onValue:(Float)->Unit){
 var width by remember{mutableFloatStateOf(1f)};val update by rememberUpdatedState(onValue)
 Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
  F320Text(label,Modifier.width(25.dp),size=11.sp,hud=true)
  Box(Modifier.weight(1f).height(29.dp).onSizeChanged{width=it.width.toFloat().coerceAtLeast(1f)}
   .pointerInput(Unit){detectHorizontalDragGestures{c,_->c.consume();update((c.position.x/width).coerceIn(0f,1f))}}
   .pointerInput(Unit){detectTapGestures{update((it.x/width).coerceIn(0f,1f))}},contentAlignment=Alignment.CenterStart){
   Canvas(Modifier.fillMaxSize()){val y=size.height/2;drawLine(Color.LightGray,Offset(0f,y),Offset(size.width,y),size.height*.22f,cap=StrokeCap.Round);drawCircle(Color.White,size.height*.43f,Offset(size.width*fraction.coerceIn(0f,1f),y))}
  }
  F320Text(right,Modifier.width(42.dp),size=10.sp,hud=true,align=TextAlign.End)
 }
}
@Composable private fun F320Ticks(modifier:Modifier,fraction:Float,onChange:(Float)->Unit){
 var width by remember{mutableFloatStateOf(1f)};val update by rememberUpdatedState(onChange)
 Box(modifier.onSizeChanged{width=it.width.toFloat().coerceAtLeast(1f)}
  .pointerInput(Unit){detectHorizontalDragGestures{c,_->c.consume();update((c.position.x/width).coerceIn(0f,1f))}}
  .pointerInput(Unit){detectTapGestures{update((it.x/width).coerceIn(0f,1f))}}){
  Canvas(Modifier.fillMaxSize()){for(i in 0..24){val x=size.width*i/24f;drawLine(Color.White.copy(alpha=.82f),Offset(x,size.height*.12f),Offset(x,if(i%2==0)size.height*.9f else size.height*.62f),if(i%2==0)1.6.dp.toPx() else 1.dp.toPx())};val x=size.width*fraction.coerceIn(0f,1f);drawLine(Color.White,Offset(x,0f),Offset(x,size.height),3.dp.toPx())}
 }
}
@Composable private fun F320FilmRow(id:String,name:String,selected:Boolean,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(7.dp)).background(if(selected)Color(0xFF3B3328) else Color(0xFF343638))
  .border(if(selected)1.5.dp else 1.dp,if(selected)F320Orange else Color(0xFF56595B),RoundedCornerShape(7.dp)).clickable(onClick=onClick).padding(horizontal=7.dp),verticalAlignment=Alignment.CenterVertically){
  F320Text("F",size=13.sp,color=if(selected)F320Orange else Color.White,hud=true);Spacer(Modifier.width(7.dp))
  F320Text(name,Modifier.weight(1f),size=11.sp,hud=true);if(selected)F320Text("✓",size=13.sp,color=F320Orange)
 }
}
@Composable private fun F320Divider(){Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha=.35f)))}

@Composable internal fun Fuji320CameraUi(
 state:Fuji320CameraUiState,actions:Fuji320CameraUiActions,preview:@Composable BoxScope.()->Unit,
 album:ImageBitmap?,leather:Painter?,logo:Painter?
){
 BoxWithConstraints(Modifier.fillMaxSize().background(if(state.light)Color(0xFFE6E6E6) else Color(0xFF08090A))){
 Box(r320(1262f,92f,640f,698f).clip(RoundedCornerShape(48.dp)).background(if(state.light)Color(0xFFD7D7D7) else Color(0xFF151617))){
  if(!state.light&&leather!=null)Image(leather,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  if(!state.light)Box(Modifier.fillMaxSize().background(Color(0x66000000)))
 }
 Canvas(Modifier.fillMaxSize()){
  val x=size.width/F320W;val y=size.height/F320H
  drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1B1D),Color(0xFF070708))))
  drawRect(Brush.verticalGradient(listOf(Color(0xFF2B2C2F),Color(0xFF0B0B0C))),size=Size(size.width,72f*y))
  drawLine(Color.White.copy(alpha=.35f),Offset(0f,72f*y),Offset(size.width,72f*y),1.5f*y)
  drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF18191B),Color(0xFF09090A))),Offset(10f*x,75f*y),Size(1275f*x,715f*y),androidx.compose.ui.geometry.CornerRadius(60f*x))
  drawRoundRect(Color.White.copy(alpha=.3f),Offset(10f*x,75f*y),Size(1275f*x,715f*y),androidx.compose.ui.geometry.CornerRadius(60f*x),style=Stroke(2f*x))
  drawRoundRect(Color(0xFF161719),Offset(1095f*x,85f*y),Size(190f*x,695f*y),androidx.compose.ui.geometry.CornerRadius(30f*x))
  drawRoundRect(Color(0xFF2C2D30),Offset(1095f*x,85f*y),Size(190f*x,695f*y),androidx.compose.ui.geometry.CornerRadius(30f*x),style=Stroke(1.5f*x))
  drawLine(Color.White.copy(alpha=.5f),Offset(133f*x,818f*y),Offset(478f*x,818f*y),2f*y)
  drawLine(Color.White.copy(alpha=.5f),Offset(823f*x,818f*y),Offset(1730f*x,818f*y),2f*y)
 }
 Box(r320(30f,92f,1075f,680f).clip(RoundedCornerShape(40.dp)).background(Color.Black)){
  preview()
  Canvas(Modifier.fillMaxSize()){
   val cx=size.width*state.afX.coerceIn(0f,1f);val cy=size.height*state.afY.coerceIn(0f,1f)
   val color=when(state.afState){1->Color(0xFF5ED08A);2->Color(0xFFE0303F);else->Color.White}
   val hw=26.dp.toPx();val hh=22.dp.toPx();val arm=14.dp.toPx();val sw=3.5.dp.toPx()
   for((dx,dy) in listOf(-1f to -1f,1f to -1f,-1f to 1f,1f to 1f)){val p=Offset(cx+dx*hw,cy+dy*hh);drawLine(color,p,Offset(p.x-dx*arm,p.y),sw,cap=StrokeCap.Round);drawLine(color,p,Offset(p.x,p.y-dy*arm),sw,cap=StrokeCap.Round)}
   drawLine(Color.White.copy(alpha=.7f),Offset(cx-62.dp.toPx(),cy),Offset(cx-35.dp.toPx(),cy),1.5.dp.toPx())
   drawLine(Color.White.copy(alpha=.7f),Offset(cx+35.dp.toPx(),cy),Offset(cx+62.dp.toPx(),cy),1.5.dp.toPx())
   drawLine(Color.White.copy(alpha=.7f),Offset(cx,cy-55.dp.toPx()),Offset(cx,cy-31.dp.toPx()),1.5.dp.toPx())
   drawLine(Color.White.copy(alpha=.7f),Offset(cx,cy+31.dp.toPx()),Offset(cx,cy+55.dp.toPx()),1.5.dp.toPx())
  }
  if(!state.hideHud)F320Text("DR100",Modifier.align(Alignment.BottomStart).padding(8.dp),size=10.sp,hud=true)
 }
 F320Button(r320(18f,17f,48f,48f),"",state.flash,actions.onFlash){F320Text("ϟ",size=25.sp,color=if(state.flash)F320Orange else Color.White)}
 F320Button(r320(74f,17f,48f,48f),"",false,actions.onHud){F320Text("☰",size=24.sp)}
 F320Button(r320(176f,17f,48f,48f),"",state.light,actions.onTheme){F320Text(if(state.light)"☾" else "☼",size=22.sp,color=if(state.light)Color.Black else Color.White)}
 Box(r320(1317f,6f,80f,80f),contentAlignment=Alignment.Center){F320Button(Modifier.fillMaxSize(),"AE-L",state.ae,actions.onAe){F320Text("AE-L",size=11.sp,color=Color.Black)}}
 Column(r320(1437f,6f,220f,66f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  F320Text("Focus : "+state.focusText,size=11.sp,hud=true);F320Ticks(Modifier.fillMaxWidth().height(24.dp),state.focus,actions.onFocus)
 }
 Box(r320(1695f,8f,76f,76f),contentAlignment=Alignment.Center){F320Button(Modifier.fillMaxSize(),"AF-L",state.af,actions.onAf){F320Text("AF-L",size=11.sp,color=Color.Black)}}
 if(!state.hideHud){
  F320Text("W",r320(335f,105f,55f,45f),size=15.sp,hud=true,align=TextAlign.Center)
  F320Ticks(r320(402f,115f,385f,25f),state.zoom,actions.onZoom)
  F320Text(state.zoomText,r320(530f,84f,150f,25f),size=10.sp,hud=true,align=TextAlign.Center)
  F320Text("T",r320(793f,105f,40f,45f),size=15.sp,hud=true,align=TextAlign.Center)
  F320Text("+3",r320(58f,205f,42f,25f),size=10.sp,hud=true)
  F320Text("+2",r320(58f,255f,42f,25f),size=10.sp,hud=true)
  F320Text("+1",r320(58f,305f,42f,25f),size=10.sp,hud=true)
  F320Text("0",r320(58f,355f,42f,25f),size=10.sp,hud=true)
  F320Text("-1",r320(58f,405f,42f,25f),size=10.sp,hud=true)
  F320Text("-2",r320(58f,455f,42f,25f),size=10.sp,hud=true)
  F320Text("-3",r320(58f,505f,42f,25f),size=10.sp,hud=true)
  Box(r320(135f,674f,945f,96f).clip(RoundedCornerShape(10.dp)).background(Color(0xD9141516)).border(1.dp,Color(0xFF3A3C3F),RoundedCornerShape(10.dp))){
   Row(Modifier.fillMaxSize().padding(horizontal=7.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1.1f),verticalArrangement=Arrangement.Center){F320Text(state.ss,Modifier,Color(0xFF2A8CFF),14.sp,hud=true)}
    F320Divider()
    Column(Modifier.weight(1.2f),verticalArrangement=Arrangement.Center){F320Text(state.iso,Modifier,Color.White,12.sp,hud=true);F320Text(state.kelvin,Modifier,Color(0xFF2A8CFF),12.sp,hud=true)}
    F320Divider()
    Column(Modifier.weight(.75f),verticalArrangement=Arrangement.Center){F320Text("EV "+String.format(Locale.US,"%.1f",state.ev),Modifier,Color.White,12.sp,hud=true)}
    F320Divider()
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.Center){F320Text(state.battery.toString()+"%",Modifier,Color.White,13.sp,hud=true);F320Text(state.batteryStatus,Modifier,Color.White,9.sp,hud=true)}
    F320Divider()
    Column(Modifier.weight(.8f),verticalArrangement=Arrangement.Center){F320Text(state.temp.toString()+"°C",Modifier,Color.White,11.sp,hud=true);F320Text("4:3",Modifier,Color.White,9.sp,hud=true)}
   }
  }
 }
 Button320(r320(1128f,140f,124f,173f),"Album",onClick=actions.onAlbum){if(album!=null)Image(album,null,Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop)else F320Text("▣",size=28.sp)}
 Button320(r320(1128f,348f,124f,167f),"BỘ LỌC",state.filterOpen||state.filter!="None",actions.onFilter){F320Text("◎",size=32.sp,color=if(state.filterOpen||state.filter!="None")F320Orange else Color.White)}
 Button320(r320(1128f,548f,124f,160f),"SETTING",onClick=actions.onSettings){F320Text("☰",size=30.sp)}
 Box(r320(1302f,96f,584f,300f).clip(RoundedCornerShape(38.dp)).background(Color(0xFF858789)).border(2.dp,Color(0xFF55585A),RoundedCornerShape(38.dp))){
  Column(Modifier.fillMaxHeight().fillMaxWidth(.55f).padding(start=8.dp,top=34.dp,bottom=38.dp),verticalArrangement=Arrangement.SpaceEvenly){
   F320Slider(Modifier.fillMaxWidth().height(42.dp),"SS",state.ss,state.ssFraction,"AUTO",actions.onSs)
   F320Slider(Modifier.fillMaxWidth().height(42.dp),"ISO",state.iso,state.isoFraction,"AUTO",actions.onIso)
   F320Slider(Modifier.fillMaxWidth().height(42.dp),"K",state.kelvin,state.kelvinFraction,"AUTO",actions.onKelvin)
  }
  if(state.filterOpen){
   Column(Modifier.align(Alignment.CenterEnd).padding(end=7.dp).width(220.dp).height(256.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFF252729)).border(2.dp,Color.Black,RoundedCornerShape(17.dp)).padding(8.dp).verticalScroll(rememberScrollState())){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){F320Text("FUJIFILM",size=13.sp,hud=true);Spacer(Modifier.weight(1f));F320Text(state.filters.size.toString()+"/"+state.filters.size,size=12.sp,color=Color(0xFF5ED08A),hud=true)}
    Spacer(Modifier.height(5.dp))
    F320FilmRow("None","Không (ảnh gốc)",state.filter=="None"){actions.onFilterSelect("None")}
    state.filters.forEach{(id,name)->F320FilmRow(id,name,state.filter==id){actions.onFilterSelect(id)}}
   }
  }else{
   Column(Modifier.align(Alignment.CenterEnd).padding(end=9.dp).width(220.dp).height(256.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFF252729)).border(2.dp,Color.Black,RoundedCornerShape(17.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    F320Text(state.luna?.let{"LunaDream "+it} ?: "LunaDream · OFF",size=14.sp,hud=true)
    state.lunaSummary.take(5).forEach{F320Text(it,size=11.sp,bold=false,hud=true)}
   }
  }
 }
 F320Text("EV",r320(1310f,337f,45f,35f),size=14.sp,hud=true)
 F320Ticks(r320(1370f,338f,265f,36f),(state.ev-state.evMin)/(state.evMax-state.evMin),actions.onEv)
 Button320(r320(1310f,418f,180f,48f),"LUNADREAM "+(state.luna ?: "3.0"),state.luna!=null,actions.onLuna){F320Text("◒",size=21.sp)}
 F320Text("Focus : "+state.focusText,r320(1503f,418f,330f,20f),size=11.sp,hud=true)
 F320Ticks(r320(1503f,440f,330f,24f),state.focus,actions.onFocus)
 Button320(r320(1720f,327f,98f,105f),"T",onClick={actions.onZoomStep(1)}){F320Text("T",size=36.sp)}
 Button320(r320(1720f,453f,98f,107f),"W",onClick={actions.onZoomStep(-1)}){F320Text("W",size=36.sp)}
 Box(r320(1368f,515f,235f,235f)){
  var pressed by remember{mutableStateOf(false)}
  val scale by animateFloatAsState(if(pressed).0.96f else 1f,tween(80),label="f320shutter")
  Canvas(Modifier.fillMaxSize().graphicsLayer{scaleX=scale;scaleY=scale}.pointerInput(Unit){detectTapGestures(onPress={
   pressed=true;actions.onShutter()
   val released=tryAwaitRelease()
   if(!released){actions.onBurstStart();tryAwaitRelease();actions.onBurstEnd()}
   pressed=false
  })}){
   val r=size.minDimension/2f
   drawCircle(Color(0x66000000),r,center+Offset(0f,r*.05f))
   drawCircle(Brush.sweepGradient(listOf(Color(0xFF8E9094),Color.White,Color(0xFF55575A),Color.White,Color(0xFF8E9094))),r*.98f)
   drawCircle(Color(0xFF17181A),r*.86f)
   drawCircle(Brush.radialGradient(listOf(Color(0xFF3B3D40),Color(0xFF111214))),r*.7f)
   drawCircle(Color(0xFF85888A),r*.7f,style=Stroke(2.dp.toPx()))
  }
  if(state.busy)F320Text("●",Modifier.align(Alignment.TopCenter),color=F320Orange,size=15.sp)
 }
 if(logo!=null)Image(logo,null,r320(540f,795f,220f,55f),contentScale=ContentScale.Fit)
 }
}

@Composable private fun F320Divider(){Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha=.35f)))}
@Composable private fun F320FilmRow(id:String,name:String,selected:Boolean,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(7.dp)).background(if(selected)Color(0xFF3B3328) else Color(0xFF343638))
  .border(if(selected)1.5.dp else 1.dp,if(selected)F320Orange else Color(0xFF56595B),RoundedCornerShape(7.dp)).clickable(onClick=onClick).padding(horizontal=7.dp),verticalAlignment=Alignment.CenterVertically){
  F320Text("F",size=13.sp,color=if(selected)F320Orange else Color.White,hud=true);Spacer(Modifier.width(7.dp))
  F320Text(name,Modifier.weight(1f),size=11.sp,hud=true);if(selected)F320Text("✓",size=13.sp,color=F320Orange)
 }
}
