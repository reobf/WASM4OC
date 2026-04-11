	#include <cstddef>
	extern "C" {
	// import host functions
	__attribute__((import_module("env"), import_name("malloc")))
	extern void* jmalloc(size_t size);
	__attribute__((import_module("env"), import_name("free")))
	extern void jfree(void* ptr);
	__attribute__((import_module("env"), import_name("calloc")))
	extern void* jcalloc(size_t a,size_t b);	
	__attribute__((import_module("env"), import_name("realloc")))
	extern void* jrealloc(void* ptr,size_t newsize);	
	// malloc&free cannot be extern, or the em++ compiler will complain
	void* malloc(size_t size) {
		return jmalloc(size); 
	}
	void free(void* ptr) {
		jfree(ptr);
	}
	void* calloc(size_t a,size_t b) {
		return jcalloc(a,b); 
	}	
	void* realloc(void* ptr,size_t newsize) {
		return jrealloc(ptr,newsize); 
	}	
	
	
	}
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
proxy = val::global("proxy");
print = val::global("print");
transposer =  proxy(std::string("1cd798bb-7b1f-4af4-a773-ad6aee2614ea"));
yield = val::global("yield");
}


//side
//1 nuke
//0 recycle
//2 rod
//3 coolant
int main() {init();
	int x=0;
	int y=0;
	while(1){
	for(x=0;x<9;x++){
		for(y=0;y<6;y++){	
			val result=transposer.call<val>("getStackInSlot", 1, x+y*9+1);
				int type=b[y][x];
				if(result[0].isNull()){
					if(type==1){
						transposer.call<val>("transferItem", 2,1, 1,getNonempty(2),x+y*9+1);
					}else{
						transposer.call<val>("transferItem", 3,1, 1,getNonempty(3),x+y*9+1);
					}
					print(type);
				}else if(result[0]["damage"].as<int>()>95&&type==1){
				
				transposer.call<val>("transferItem", 1,0, 1,x+y*9+1);
				transposer.call<val>("transferItem", 2,1, 1,getNonempty(2),x+y*9+1);

				}
			
	}
	}
	yield();
	}
	

	

	
	
	

	return 0;
}