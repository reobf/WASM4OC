#include <cstddef>
extern "C" {
__attribute__((import_module("env"), import_name("malloc")))
extern void* jmalloc(size_t size);
__attribute__((import_module("env"), import_name("free")))
extern void jfree(void* ptr);
extern void free(void* ptr);
void* malloc(size_t size) {
	return jmalloc(size); 
}
void free(void* ptr) {
	jfree(ptr);
}
}
#include <emscripten/val.h>
#include <string>



using namespace emscripten;

int main() {
	while(1){
	val transposer =  val::global("proxy")(std::string("b4164cbb-1a4f-4b63-ab87-8403b75cdd89"));
	val result=transposer.call<val>("getStackInSlot", 1, 1);
	if(!result[0].isNull()){
        val::global("print")(result[0]);
        val::global("print")(result[0]["name"]);
		std::string name=result[0]["name"].as<std::string>();
        puts(name.c_str());   
	}else{
		val::global("print")(std::string("empty!"));
	
	}
	}
	return 0;
}