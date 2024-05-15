#include <ctime>
#include <cstdio>
#include <unistd.h>
#include <fcntl.h>
#include <cassert>
#include <jni.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <sstream>
#include <iomanip>
#include <cerrno> // Include errno for error handling

class elapsedTime {
private:
    std::chrono::steady_clock::time_point start_time;
    bool is_running;

public:
    elapsedTime() : is_running(false) {}

    void start() {
        if (!is_running) {
            start_time = std::chrono::steady_clock::now();
            is_running = true;
        }
    }

    void pause() {
        if (is_running) {
            is_running = false;
        }
    }

    double elapsed_time() {
        if (is_running) {
            return std::chrono::duration<double>(std::chrono::steady_clock::now() - start_time).count();
        } else {
            return 0.0;
        }
    }

    // getter & setter
    bool is_running_GET(){
        return is_running;
    }
};

elapsedTime elapsedTimer;

void updateTimerAndSegmentDisplay(JNIEnv *env, jobject instance, jobject textView) {
    jclass textViewClass = env->GetObjectClass(textView);
    jmethodID setTextMethod = env->GetMethodID(textViewClass, "setText", "(Ljava/lang/CharSequence;)V");

    int fd;
    char nums[7];
    struct tm *tm_ptr;
    time_t the_time;

    while (elapsedTimer.is_running_GET()) {
        // Calculate elapsed time
        double elapsed = elapsedTimer.elapsed_time();
        int minutes = static_cast<int>(elapsed / 60);
        int seconds = static_cast<int>(elapsed) % 60;

        // Format elapsed time
        std::ostringstream ss;
        ss << std::setw(2) << std::setfill('0') << minutes << ":" << std::setw(2) << std::setfill('0') << seconds;
        jstring text = env->NewStringUTF(ss.str().c_str());

        // Update TextView on UI thread
        env->CallVoidMethod(instance, setTextMethod, text);

        // Release local reference
        env->DeleteLocalRef(text);

        // Update segment display
        fd = open("/dev/fpga_segment", O_WRONLY);
        if (fd < 0) {
            // Error opening segment device
            std::cerr << "Error opening segment driver: " << strerror(errno) << std::endl;
        } else {
            // Calculate elapsed time
            double elapsed = elapsedTimer.elapsed_time();
            int minutes = static_cast<int>(elapsed / 60);
            int seconds = static_cast<int>(elapsed) % 60;

            // Format elapsed time
            sprintf(nums, "%02d%02d", minutes, seconds);
            // Alternatively, if you have only one digit for minutes, you can use:
            // sprintf(nums, "%01d%02d%02d", minutes, seconds / 10, seconds % 10);

            // Write to segment device
            if (write(fd, nums, 6) < 0) {
                // Error writing to segment device
                std::cerr << "Error writing to segment driver: " << strerror(errno) << std::endl;
            }

            close(fd);
        }

        // Sleep for 1 second
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_startTimer(JNIEnv *env, jobject instance, jobject textView) {
    elapsedTimer.start();
    std::thread(updateTimerAndSegmentDisplay, env, instance, textView).detach();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_pauseTimer(JNIEnv *env, jobject instance) {
    elapsedTimer.pause();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hanbackmusicapp_MainActivity_resetTimer(JNIEnv *env, jobject instance) {
    elapsedTimer.pause();
}
