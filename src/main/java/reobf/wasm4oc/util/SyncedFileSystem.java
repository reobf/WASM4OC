package reobf.wasm4oc.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.NBTTagCompound;

public class SyncedFileSystem implements FileSystem{
	public  SyncedFileSystem(FileSystem wrapped,Supplier<Boolean> isAlivePredicate) {this.wrapped=wrapped;
	this.isAlivePredicate=isAlivePredicate;}
	Supplier<Boolean> isAlivePredicate;
	FileSystem wrapped;
	
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void throwUnchecked(Throwable exception) throws E {
	    throw (E) exception;
	}
	
	@Override
	public void load(NBTTagCompound nbt) {
		
		
	}

	@Override
	public void save(NBTTagCompound nbt) {
		
		
	}
	
	@Override
	public boolean isReadOnly() {synchronized (wrapped) {
		
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.isReadOnly();}
	}

	@Override
	public long spaceTotal() {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.spaceTotal();}
	}

	@Override
	public long spaceUsed() {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.spaceUsed();}
	}

	@Override
	public boolean exists(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.exists(path);}
	}

	@Override
	public long size(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.size(path);}
	}

	@Override
	public boolean isDirectory(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.isDirectory(path);}
	}

	@Override
	public long lastModified(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return wrapped.lastModified(path);}
	}

	@Override
	public String[] list(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.list(path);}
	}

	@Override
	public boolean delete(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.delete(path);}
	}

	@Override
	public boolean makeDirectory(String path) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.makeDirectory(path);}
	}

	@Override
	public boolean rename(String from, String to) throws FileNotFoundException {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.rename(from,to);}
	}

	@Override
	public boolean setLastModified(String path, long time) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.setLastModified(path,time);}
	}

	@Override
	public int open(String path, Mode mode) throws FileNotFoundException {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.open(path,mode);}
	}

	@Override
	public Handle getHandle(int handle) {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		return  wrapped.getHandle(handle);}
	}

	@Override
	public void close() {synchronized (wrapped) {
		if(!isAlivePredicate.get())throwUnchecked(new IOException());
		wrapped.close();}
		
	}

}
