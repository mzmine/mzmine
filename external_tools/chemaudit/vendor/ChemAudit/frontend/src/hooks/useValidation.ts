import { useState, useCallback } from 'react';
import { validationApi } from '../services/api';
import type { ValidationRequest, ValidationResponse, ValidationError } from '../types/validation';

interface UseValidationResult {
  validate: (request: ValidationRequest) => Promise<void>;
  result: ValidationResponse | null;
  error: ValidationError | null;
  isLoading: boolean;
  reset: () => void;
}

export function useValidation(): UseValidationResult {
  const [result, setResult] = useState<ValidationResponse | null>(null);
  const [error, setError] = useState<ValidationError | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const validate = useCallback(async (request: ValidationRequest) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await validationApi.validate(request);
      setResult(response);
    } catch (err) {
      setError(err as ValidationError);
      setResult(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setResult(null);
    setError(null);
    setIsLoading(false);
  }, []);

  return { validate, result, error, isLoading, reset };
}
