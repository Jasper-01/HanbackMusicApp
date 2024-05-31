#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <jni.h>
#include "h_textLCDioctl.h"
#include <string.h>
#include <string>
#include <algorithm>

#define MAX_CHAR_LCD 16  // Assuming the LCD can display 16 characters per line

// Function to validate if a character is supported by the text LCD
bool isValidChar(char c) {
    // Define the valid character range or set of characters here
    // For simplicity, let's assume only ASCII printable characters are valid
    return (c >= 32 && c <= 126);  // ASCII printable characters range
}

// Function to process the input string and skip invalid characters
void filterInvalidChars(const char* input, char* output, size_t maxLen) {
    size_t outIndex = 0;
    for (size_t i = 0; input[i] != '\0' && outIndex < maxLen; ++i) {
        if (isValidChar(input[i])) {
            output[outIndex++] = input[i];
        }
    }
    output[outIndex] = '\0';  // Null-terminate the output string
}

// Function to remove specific words and characters from the input string
std::string sanitizeString(std::string str, const std::string& channelName) {
    // Remove "MV"
    size_t pos;
    while ((pos = str.find("MV")) != std::string::npos) {
        str.erase(pos, 2);
    }
    // Remove channel name
    while ((pos = str.find(channelName)) != std::string::npos) {
        str.erase(pos, channelName.length());
    }
    // Remove brackets
    str.erase(std::remove(str.begin(), str.end(), '['), str.end());
    str.erase(std::remove(str.begin(), str.end(), ']'), str.end());
    str.erase(std::remove(str.begin(), str.end(), '('), str.end());
    str.erase(std::remove(str.begin(), str.end(), ')'), str.end());
    // Remove dashes
    str.erase(std::remove(str.begin(), str.end(), '-'), str.end());
    // Remove leading spaces
    str.erase(0, str.find_first_not_of(' '));

    return str;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_textLCDout(
        JNIEnv* env,
        jobject /* this */,
        jstring data1,
        jstring data2) {

    int fd;

    // Convert jstring to std::string
    const char* rawLine1 = env->GetStringUTFChars(data1, nullptr);
    const char* rawLine2 = env->GetStringUTFChars(data2, nullptr);
    assert(rawLine1 != nullptr);
    assert(rawLine2 != nullptr);

    std::string strLine1(rawLine1);
    std::string strLine2(rawLine2);

    // Sanitize the input string
    std::string sanitizedStrLine1 = sanitizeString(strLine1, strLine2);

    // Create buffers to store filtered strings
    char line1[MAX_CHAR_LCD + 1] = {0};  // +1 for null-terminator
    char line2[MAX_CHAR_LCD + 1] = {0};

    // Copy and filter input strings
    filterInvalidChars(sanitizedStrLine1.c_str(), line1, MAX_CHAR_LCD);
    filterInvalidChars(rawLine2, line2, MAX_CHAR_LCD);

    // Open the device
    fd = open("/dev/fpga_textlcd", O_WRONLY);
    if (fd == -1) {
        perror("Failed to open /dev/fpga_textlcd");
        // Release the strings
        env->ReleaseStringUTFChars(data1, rawLine1);
        env->ReleaseStringUTFChars(data2, rawLine2);
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
    env->ReleaseStringUTFChars(data1, rawLine1);
    env->ReleaseStringUTFChars(data2, rawLine2);
}
