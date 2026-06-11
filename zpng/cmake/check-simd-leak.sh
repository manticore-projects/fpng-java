#!/bin/sh
# check-simd-leak.sh — fail if AVX/VEX instructions appear in any object
# other than zpng_kernels_avx2.o.
#
# Usage: check-simd-leak.sh <objdump> <object>...
#
# Rationale: zpng selects SIMD kernels at runtime via CPUID. That only
# protects non-AVX CPUs (e.g. VirtualBox guests with AVX masked from
# CPUID) if AVX instructions are physically confined to the one TU that
# is gated behind the CPUID check. A stray -mavx2 / -march=native on any
# other TU lets the compiler auto-vectorize plain loops with VEX/ymm
# instructions and reintroduces the SIGILL — silently, until the binary
# hits a non-AVX machine. This script turns that silent regression into
# a build failure.
#
# Detection: any ymm register reference, or any VEX-prefixed mnemonic
# (all v-prefixed SIMD mnemonics: vmovdqu, vpshufb, vzeroupper, ...).
# Legacy SSE encodings (movdqu, pshufb) do not match and are allowed —
# the sse41 TU relies on that.

set -eu

if [ "$#" -lt 2 ]; then
    echo "usage: $0 <objdump> <object>..." >&2
    exit 2
fi

OBJDUMP="$1"
shift

PATTERN='%ymm|[[:space:]]v(mov|pshu|pbroad|punpck|pinsr|pextr|por|pand|pxor|padd|psub|insert|extract|blend|perm|zeroupper|broadcast)'

fail=0
for obj in "$@"; do
    case "$obj" in
        *zpng_kernels_avx2*) continue ;;   # the one TU allowed to contain AVX
    esac
    if "$OBJDUMP" -d "$obj" 2>/dev/null | grep -qE "$PATTERN"; then
        echo "ERROR: AVX/VEX instructions leaked into baseline TU: $obj" >&2
        echo "       (first occurrences below — check this TU's compile flags" >&2
        echo "        against the per-TU table in CMakeLists.txt / zpng_kernels.h)" >&2
        "$OBJDUMP" -d "$obj" | grep -E "$PATTERN" | head -5 >&2
        fail=1
    fi
done

if [ "$fail" -ne 0 ]; then
    echo "zpng SIMD leak check FAILED — build aborted." >&2
    exit 1
fi
echo "zpng SIMD leak check OK: no AVX/VEX outside zpng_kernels_avx2.o"
