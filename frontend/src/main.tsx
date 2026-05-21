import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'sonner';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
      <Toaster
        position="bottom-right"
        toastOptions={{
          style: {
            background: '#1C1C2E',
            border: '1px solid #3A3A5C',
            color: '#F5F5F7',
            fontSize: '0.875rem',
          },
        }}
        richColors
      />
    </BrowserRouter>
  </React.StrictMode>,
);
