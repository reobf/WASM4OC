__attribute__((import_module("env"), import_name("print")))
extern void print(int v);

int main() {
    
	int i=0;
	while(1){
	i++;
	// you can save the game at any time
	// when you load it again, it will resume instead of starting from 0
    print(i)
	}
	
    return 0;
}