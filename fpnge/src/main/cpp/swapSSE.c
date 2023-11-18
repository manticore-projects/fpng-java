#include <stdio.h>
#include <stdlib.h>
#include <immintrin.h>  // For AVX
#include <stdlib.h>
#include <string.h>


void swapChannelsABGRtoRGBA_inplace(void* pImage, int numPixels) {
    const __m128i shuffleMask = _mm_set_epi8( 12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3);
    unsigned char* src = (unsigned char*)pImage;

    for (int i = 0; i < numPixels; i += 4) {
        __m128i abgr = _mm_load_si128((__m128i*)(src + i * 4));
        __m128i rgba = _mm_shuffle_epi8(abgr, shuffleMask);
        _mm_store_si128((__m128i*)(src + i * 4), rgba);
    }
}

void printPixels(const unsigned char* pixels, int numPixels) {
    for (int i = 0; i < numPixels; ++i) {
        printf("(%d, %d, %d, %d) ", pixels[i * 4], pixels[i * 4 + 1], pixels[i * 4 + 2], pixels[i * 4 + 3]);
    }
    printf("\n");
}

int main() {
    //int numPixels = 2780976;  // Adjust as needed
    int numPixels = 16;  // Adjust as needed

    size_t pixelSize = 4; // Assuming 4 bytes per pixel

    // Create a sample unaligned ABGR array

    unsigned char* abgrArray = (unsigned char*) malloc(numPixels * 4);
    for (int i = 0; i < numPixels * 4; i += 4) {
        abgrArray[i] = i;       // Blue channel
        abgrArray[i + 1] = i + 1; // Green channel
        abgrArray[i + 2] = i + 2; // Red channel
        abgrArray[i + 3] = i + 3; // Alpha channel
    }

    // Print original pixels
    printf("Original Pixels: ");
    printPixels(abgrArray, numPixels);

    // Call the channel swapping function
    swapChannelsABGRtoRGBA_inplace(abgrArray, numPixels);

    // Print modified pixels
    printf("Modified Pixels: ");
    printPixels(abgrArray, numPixels);

    free(abgrArray);

    return 0;
}