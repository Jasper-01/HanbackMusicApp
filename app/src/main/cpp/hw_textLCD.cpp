#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <jni.h>
#include "h_textLCDioctl.h"
#include <string.h>

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_textLCDout(
        JNIEnv* env,
        jobject /* this */,
        jstring data1,
        jstring data2){

    int fd;

    // Convert jstring to const char *
    const char* line1 = env->GetStringUTFChars(data1, nullptr);
    const char* line2 = env->GetStringUTFChars(data2, nullptr);

    // Check if conversion was successful
    assert(line1 != nullptr);
    assert(line2 != nullptr);

    fd = open("/dev/fpga_textlcd", O_WRONLY);
//    assert(fd != -1);
    if (fd == -1) {
        perror("Failed to open /dev/fpga_textlcd");
        // Release the strings
        env->ReleaseStringUTFChars(data1, line1);
        env->ReleaseStringUTFChars(data2, line2);
        return; // Exit the function if the file failed to open
    }

    ioctl(fd, TEXTLCD_INIT);

    ioctl(fd, TEXTLCD_CLEAR);
    ioctl(fd, TEXTLCD_LINE1);
    write(fd, line1, strlen(line1));
    ioctl(fd, TEXTLCD_LINE2);
    write(fd, line2, strlen(line2));
    close(fd);

    // Release the strings
    env->ReleaseStringUTFChars(data1, line1);
    env->ReleaseStringUTFChars(data2, line2);
}