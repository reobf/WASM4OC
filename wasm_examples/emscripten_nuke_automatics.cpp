#include <emscripten/val.h>
#include <string>

using namespace emscripten;
val proxy ;
val print ;
val transposer ;
val yield ;
int b[][9]={
{1,0,0,0,1,0,0,0,1},
{0,0,1,0,0,0,1,0,0},
{1,0,0,0,1,0,0,0,1},
{0,0,1,0,0,0,1,0,0},
{1,0,0,0,1,0,0,0,1},
{0,0,1,0,0,0,1,0,0}
};// 1 coolant 0 rod


int getNonempty(int side){
	int size=transposer.call<val>("getInventorySize",side)[0].as<int>();
	int i=0;
	for(i=0;i<size;i++){
		val get=transposer.call<val>("getStackInSlot", side, i+1);
		if(!get[0].isNull()){
			return i+1;
		}
	}
	return -1;
	
}
void init(){
//proxy = val::global("proxy");
//transposer =  proxy(std::string("1cd798bb-7b1f-4af4-a773-ad6aee2614ea"));
transposer = val::global("component").call<val>("ofType","transposer")[0];
print = val::global("print");
yield = val::global("yield");
}
// ic2 nuke update every 20 ticks
// you can set this to 0 for better performance
int everyTick=1;

//side
//1 nuke
//0 recycle
//2 rod
//3 coolant
int main() {
	
	int count=0;
	
	init();
	int x=0;
	int y=0;
	while(1){
	if(everyTick){
		if(++count>20){
			yield();
		}count=0;
	}
	
	for(x=0;x<9;x++){
		for(y=0;y<6;y++){	
			val result=transposer.call<val>("getStackInSlot", 1, x+y*9+1);
				int type=b[y][x];
				if(result[0].isNull()){
					// remember to keep rod & coolant in stock! Or the program will crash if fails to find one!
					if(type==1){
						transposer.call<val>("transferItem", 2,1, 1,getNonempty(2),x+y*9+1);//refill coolant
					}else{
						transposer.call<val>("transferItem", 3,1, 1,getNonempty(3),x+y*9+1);//refill rod
						// deplated rods removing is not implemented
						// do it with EIO conduit or something
						// removing rods is safe and latency-insensitive
					}
					print(type);
				}else if(result[0]["damage"].as<int>()>95&&type==1){
				// near-broken coolant
				transposer.call<val>("transferItem", 1,0, 1,x+y*9+1);// move to recycle
				transposer.call<val>("transferItem", 2,1, 1,getNonempty(2),x+y*9+1);// refill

				}
			
	}
	}
	yield();
	}

	
	
	

	return 0;
}