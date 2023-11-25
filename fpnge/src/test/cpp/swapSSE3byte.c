#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <immintrin.h>  // For AVX


void swapBlueGreenSSE2(unsigned char* pImage, int numPixels) {
    // Ensure numPixels is a multiple of 4 for alignment
    int numIterations = numPixels / 4;

    // 3 * 4 = 12, so don't shuffle the last 4 bytes
    const __m128i shuffleMask = _mm_set_epi8( 15, 14, 13, 12, 9, 10, 11, 6, 7, 8, 3, 4, 5, 0, 1, 2 );
    for (int i = 0; i < numIterations; ++i) {
        // Load 5 pixels (5 * 3 bytes + 1 = 16) at a time
        __m128i pixels = _mm_loadu_si128((__m128i*)(pImage + i * 4 * 3));

        // Shuffle bytes to swap blue and green channels for each pixel
        __m128i shuffled = _mm_shuffle_epi8(pixels, shuffleMask);

        // Store the result back to memory
        _mm_storeu_si128((__m128i*)(pImage + i * 4 * 3), shuffled);
    }
}

void printPixels(const unsigned char* pixels, int numPixels) {
    for (int i = 0; i < numPixels; ++i) {
        printf("(%d, %d, %d) ", pixels[i * 3], pixels[i * 3 + 1], pixels[i * 3 + 2]);
    }
    printf("\n");
}

int main() {
    int numPixels = 16;  // Adjust as needed
    size_t pixelSize = 3; // Assuming 4 bytes per pixel

    // Create a sample unaligned ABGR array

    unsigned char* abgrArray = (unsigned char*) malloc(numPixels * 3);
    for (int i = 0; i < numPixels*3; i += 3) {
        abgrArray[i] = i;       // Blue channel
        abgrArray[i + 1] = i + 1; // Green channel
        abgrArray[i + 2] = i + 2; // Red channel
    }

    // Print original pixels
    printf("Original Pixels: ");
    printPixels(abgrArray, numPixels);

    // Call the channel swapping function
    swapBlueGreenSSE2(abgrArray, numPixels);

    // Print modified pixels
    printf("Modified Pixels: ");
    printPixels(abgrArray, numPixels);

    free(abgrArray);

    return 0;
}