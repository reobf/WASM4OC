#include <string.h>
#include <stdlib.h>

// cstring to Java String handle
__attribute__((import_module("env"), import_name("cstring")))
extern __externref_t cstring(void* ptr);
// Java String handle to cstring
__attribute__((import_module("env"), import_name("wasm_cstring")))
extern char* wasm_cstring(__externref_t handle, void* (*mallocp)(int));
// print a signed integer
__attribute__((import_module("env"), import_name("print")))
extern void print(int v);
// print a Java handle
__attribute__((import_module("env"), import_name("printJava")))
extern void printJava(__externref_t v);
// special malloc function, allocate a special mem block with the highest bit=1, more efficient than C impl
__attribute__((import_module("env"), import_name("malloc")))
extern void* jmalloc(int v);
// corresonding free function
__attribute__((import_module("env"), import_name("free")))
extern void jfree(void* v);





int main() {
    
	char* original = "hello world";
	while(1){
    __externref_t handle = cstring(original);
    printJava(handle);
    char* returned_ptr = wasm_cstring(handle, jmalloc);
    

    int same = (strcmp(original, returned_ptr) == 0) ? 1 : 0;
    print(same);
    jfree(returned_ptr);
	}
	
    return 0;
}