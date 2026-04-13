#include <stdlib.h>


__attribute__((import_module("env"), import_name("int")))
extern __externref_t toInt(int ptr);
__attribute__((import_module("env"), import_name("cstring")))
extern __externref_t cstring(void* ptr);
__attribute__((import_module("env"), import_name("printJava")))
extern void printJava(__externref_t v);
extern void jfree(void* v);
__attribute__((import_module("env"), import_name("pack4")))
extern __externref_t pack4(__externref_t a1,__externref_t a2,__externref_t a3,__externref_t a4);
__attribute__((import_module("env"), import_name("get")))
extern __externref_t get(__externref_t v,int index
);
__attribute__((import_module("env"), import_name("OC_invoke")))
extern __externref_t OC_invoke(__externref_t);



__externref_t args;
int main() {
    char* address="b4164cbb-1a4f-4b63-ab87-8403b75cdd89";// place your address here
	char* method="getStackInSlot";


	__externref_t pack=pack4(cstring(address),cstring(method),toInt(1),toInt(1));// 1:upside 1:first slot
	// get the item in the first slot of the chest on the transposer
	__externref_t result=OC_invoke(pack);
	printJava(result);
    return 123;
}