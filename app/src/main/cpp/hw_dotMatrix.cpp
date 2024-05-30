#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <jni.h>
#include "h_dotMatrixFont.h"

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_dotMatrixOut(
        JNIEnv* env,
        jobject /* this */,
        jstring givenDat){
    int dev, i, j, offset = 20, ch, len;
    char result[600], tmp[2];
    const char* input;

    dev = open("/dev/fpga_dotmatrix", O_WRONLY);

    if (dev != -1) {
        input = env->GetStringUTFChars(givenDat, nullptr);
        len = strlen(input);
        for (j = 0; j < 20; j++)
            result[j] = '0';

        for (i = 0; i < len; i++) {
            ch = input[i];

            ch -= 0x20;

            for (j = 0; j < 5; j++) {
                sprintf(tmp, "%x%x", font[ch][j] / 16, font[ch][j] % 16);

                result[offset++] = tmp[0];
                result[offset++] = tmp[1];
            }
            result[offset++] = '0';
            result[offset++] = '0';
        }

        for (j = 0; j < 20; j++)
            result[offset++] = '0';

        for (i = 0; i < (offset - 18) / 2; i++) {
            for (j = 0; j < 20; j++) {
                write(dev, &result[2 * i], 20);
            }
        }
    } else {
        return;
    }
    close(dev);
}