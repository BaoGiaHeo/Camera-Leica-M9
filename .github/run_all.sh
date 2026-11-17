#!/bin/sh
# Needs: python3 + numpy. Each script prints True/False for bit-exact equality.
cd "$(dirname "$0")"
python3 ref.py && python3 fused.py && python3 fusion_check.py
