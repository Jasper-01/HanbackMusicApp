#include <jni.h>
#include <string>
#include <chrono>
#include <thread>
#include <atomic>

bool running = false;
std::chrono::steady_clock::time_point start_time;
std::atomic<long> paused_time_elapsed(0);

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_startTimer(JNIEnv *env, jobject /* this */) {
if (!running) {
    start_time = std::chrono::steady_clock::now();
    running = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_stopTimer(JNIEnv *env, jobject /* this */) {
if (running) {
    auto now = std::chrono::steady_clock::now();
    paused_time_elapsed += std::chrono::duration_cast<std::chrono::seconds>(now - start_time).count();
    running = false;
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_resetTimer(JNIEnv *env, jobject /* this */) {
    running = false;
    paused_time_elapsed = 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_hanbackmusicapp_MainActivity_getElapsedTime(JNIEnv *env, jobject /* this */) {
    long elapsed = paused_time_elapsed.load();
    if (running) {
        auto now = std::chrono::steady_clock::now();
        elapsed += std::chrono::duration_cast<std::chrono::seconds>(now - start_time).count();
    }

    int minutes = elapsed / 60;
    int seconds = elapsed % 60;
    char buffer[6];
    snprintf(buffer, sizeof(buffer), "%02d:%02d", minutes, seconds);
    return env->NewStringUTF(buffer);
}
