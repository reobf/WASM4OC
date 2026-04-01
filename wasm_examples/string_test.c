#include <string.h>
#include <stdlib.h>
#include <stdint.h>
// cstring to Java String handle
__attribute__((import_module("env"), import_name("cstring")))
extern int cstring(int ptr);
// Java String handle to cstring
__attribute__((import_module("env"), import_name("wasm_cstring")))
extern int wasm_cstring(int handle, int malloc_func);
// print a signed integer
__attribute__((import_module("env"), import_name("print")))
extern void print(int v);
// print a Java handle
__attribute__((import_module("env"), import_name("printJava")))
extern void printJava(int v);
// special malloc function, allocate a special mem block with the highest bit=1, more efficient than C impl
__attribute__((import_module("env"), import_name("malloc")))
extern void* jmalloc(int v);
// corresonding free function
__attribute__((import_module("env"), import_name("free")))
extern void jfree(void* v);





int main() {
    
	const char* original = "hello world";
	while(1){
    int handle = cstring((int)(intptr_t)original);
    printJava(handle);
    int returned_ptr = wasm_cstring(handle, (int)(intptr_t)jmalloc);
    

    int same = (strcmp(original, (const char*)(intptr_t)returned_ptr) == 0) ? 1 : 0;
    print(same);
    jfree((void*)(intptr_t)returned_ptr);
	}
	
    return 0;
}