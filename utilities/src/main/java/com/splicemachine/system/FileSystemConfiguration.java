/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.splicemachine.system;

import org.spark_project.guava.base.Function;
import org.spark_project.guava.collect.Iterables;
import org.spark_project.guava.collect.Lists;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Overall representation of the configuration of the file system.
 *
 * @author Scott Fines
 *         Date: 1/13/15
 */
public class FileSystemConfiguration implements SystemConfiguration{
    private final FileSystem localFileSystem;

    public FileSystemConfiguration(){
        this(FileSystems.getDefault());
    }

    public FileSystemConfiguration(FileSystem localFileSystem) {
        this.localFileSystem = localFileSystem;
    }

    public long totalDiskSpaceBytes() throws IOException {
        Iterable<FileStore> fileStores = localFileSystem.getFileStores();
        long totalDiskSpaceBytes = 0l;
        for(FileStore fs:fileStores){
            totalDiskSpaceBytes+=fs.getTotalSpace();
        }
        return totalDiskSpaceBytes;
    }

    public long freeDiskBytes() throws IOException {
        Iterable<FileStore> fileStores = localFileSystem.getFileStores();
        long totalUsableSpace = 0l;
        for(FileStore fs:fileStores){
            totalUsableSpace+=fs.getUsableSpace();
        }
        return totalUsableSpace;
    }

    public long unallocatedBytes() throws IOException {
        Iterable<FileStore> fileStores = localFileSystem.getFileStores();
        long totalUsableSpace = 0l;
        for(FileStore fs:fileStores){
            totalUsableSpace+=fs.getUnallocatedSpace();
        }
        return totalUsableSpace;
    }

    public boolean isReadOnly(){
        for(FileStore fs:localFileSystem.getFileStores()){
            if(!fs.isReadOnly()) return false;
        }
        return true;
    }

    public int numRootDirectories(){
        int count = 0;
        for(Path root:localFileSystem.getRootDirectories()){
            count++;
        }
        return count;
    }

    private static final Function<Path,String> pathNameFunction = new Function<Path, String>() {
        @Override
        public String apply(Path input) {
            return input.toAbsolutePath().toString();
        }
    };

    public Collection<String> rootDirectories(){
        return Lists.newArrayList(Iterables.transform(localFileSystem.getRootDirectories(),pathNameFunction));
    }

    public int numFileStores(){
        int count = 0;
        for(FileStore fs:localFileSystem.getFileStores()){
            count++;
        }
        return count;
    }


    @Override
    public String prettyPrint() throws IOException {

        return null;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static void main(String...args) throws Exception{
        FileSystemConfiguration config = new FileSystemConfiguration();
        long tDS = config.totalDiskSpaceBytes();
        long fDs = config.freeDiskBytes();
        long uDs = config.unallocatedBytes();
        long s = tDS;
        int scale = 0;
        while(s>1024){
            s>>=10;
            scale++;
        }
        String scaleLabel = Scales.abbByteLabel(scale);
        System.out.printf("Total Disk Space: %f %s%n",Scales.scaleBytes(tDS,scale),scaleLabel);
        System.out.printf("Free Disk Space: %f %s%n",Scales.scaleBytes(fDs,scale),scaleLabel);
        System.out.printf("Unallocated Disk Space: %f %s%n",Scales.scaleBytes(uDs,scale),scaleLabel);
        System.out.printf("Number of File Stores: %d%n",config.numFileStores());
        System.out.printf("Number of Root Directories: %d%n",config.numRootDirectories());
        System.out.printf("Is Read Only: %b%n",config.isReadOnly());
        System.out.printf("Root Directories: %s%n",config.rootDirectories());
    }
}
