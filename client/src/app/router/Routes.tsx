import { createBrowserRouter } from "react-router-dom";
import App from "../layout/App";
import HomePage from "../../features/HomePage";
import Catalog from "../../features/Catalog";
import ContactPage from "../../features/ContactPage";
import ProductDetails from "../../features/ProductDetails";

export const router = createBrowserRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { path: '', element: <HomePage /> },
            { path: 'store', element: <Catalog /> },
            { path: 'store/:id', element: <ProductDetails /> },
            { path: 'contact', element: <ContactPage /> },
        ]
    }
])