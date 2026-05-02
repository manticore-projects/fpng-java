#include <stdio.h>
#include <stdlib.h>
#include <immintrin.h>  // For AVX
#include <stdlib.h>
#include <string.h>

void copyUnalignedToAligned(void* src, void** alignedDst, size_t size, size_t alignment) {
    // Allocate aligned memory
    *alignedDst = _mm_malloc(size, alignment);

    // Copy unaligned data to aligned buffer
    memcpy(*alignedDst, src, size);
}

void freeAligned(void* alignedPtr) {
    // Free aligned memory
    _mm_free(alignedPtr);
}

void swapChannelsABGRtoRGBA_AVX_inplace(void* pImage, int numPixels ) {
      const __m256i shuffleMask = _mm256_set_epi8(
              //15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
              //15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0
              12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3,
              12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3
          );

   unsigned char* src = (unsigned char*)pImage;

   for (int i = 0; i < numPixels; i += 8) {
           __m256i abgr = _mm256_load_si256((__m256i*)(src + i * 4));
           __m256i rgba = _mm256_shuffle_epi8(abgr, shuffleMask);
           _mm256_store_si256((__m256i*)(src + i * 4), rgba);
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

    // Align the array
    unsigned char* alignedArray;
    copyUnalignedToAligned(abgrArray, (void**)&alignedArray, numPixels * pixelSize, 32);

    // Print original pixels
    printf("Original Pixels: ");
    printPixels(abgrArray, numPixels);

    // Call the channel swapping function
    swapChannelsABGRtoRGBA_AVX_inplace(alignedArray, numPixels);

    // Print modified pixels
    printf("Modified Pixels: ");
    printPixels(alignedArray, numPixels);

    freeAligned(abgrArray);
    freeAligned(alignedArray);

    return 0;
}