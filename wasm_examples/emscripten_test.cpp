#include <emscripten/val.h>
#include <string>



using namespace emscripten;

int main() {
	while(1){
	val proxy = val::global("proxy");
	val transposer =  proxy(std::string("b4164cbb-1a4f-4b63-ab87-8403b75cdd89"));
	val result=transposer.call<val>("getStackInSlot", 1, 1);
	if(!result[0].isNull()){
        val::global("print")(result[0]);
        val::global("print")(result[0]["name"]);
		std::string name=result[0]["name"].as<std::string>();
		// convert to std::string to manipulate it in C++!
		// you cannot call length() on a emscripten val type
		val::global("print")(std::string("Length: ")+std::to_string(name.length()));
	}else{
		val::global("print")(std::string("empty!"));
	
	}
	}
	return 0;
}